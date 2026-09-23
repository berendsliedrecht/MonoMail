package com.fsck.k9.ui.eink

import android.content.ContentValues
import android.content.Context
import android.graphics.Typeface
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.SpannableStringBuilder
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.text.style.URLSpan
import android.text.style.UnderlineSpan
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import androidx.core.text.HtmlCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.k9mail.legacy.message.controller.MessageReference
import app.k9mail.legacy.message.controller.SimpleMessagingListener
import app.k9mail.legacy.ui.folder.DisplayFolder
import app.k9mail.legacy.ui.folder.DisplayFolderRepository
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.mail.Address
import com.fsck.k9.mail.Message
import com.fsck.k9.mail.Part
import com.fsck.k9.mail.internet.MimeMessage
import com.fsck.k9.message.MessageBuilder
import com.fsck.k9.message.QuotedTextMode
import net.thunderbird.core.android.account.QuoteStyle
import com.fsck.k9.message.SimpleMessageBuilder
import com.fsck.k9.message.SimpleMessageFormat
import com.fsck.k9.mailstore.AttachmentViewInfo
import com.fsck.k9.mailstore.LocalMessage
import com.fsck.k9.mailstore.MessageViewInfoExtractorFactory
import java.io.File
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import com.fsck.k9.ui.messagelist.MessageListConfig
import com.fsck.k9.ui.messagelist.MessageListInfo
import com.fsck.k9.ui.messagelist.MessageListItem
import com.fsck.k9.ui.messagelist.MessageListLiveData
import com.fsck.k9.ui.messagelist.MessageListLiveDataFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.common.exception.MessagingException
import net.thunderbird.core.common.mail.Flag
import net.thunderbird.core.android.account.LegacyAccountDtoManager
import net.thunderbird.core.android.account.SortType
import net.thunderbird.core.common.mail.html.HtmlSettings
import net.thunderbird.core.preference.GeneralSettings
import net.thunderbird.core.preference.GeneralSettingsManager
import net.thunderbird.core.preference.update
import net.thunderbird.feature.search.legacy.LocalMessageSearch
import net.thunderbird.feature.search.legacy.SearchAccount
import net.thunderbird.feature.search.legacy.api.MessageSearchField
import net.thunderbird.feature.search.legacy.api.SearchAttribute
import net.thunderbird.feature.search.legacy.api.SearchCondition

/** Which list is currently shown in the message list screen. */
internal data class FolderSelection(
    val accountUuid: String?,
    val folderId: Long?,
    val name: String,
    val isUnified: Boolean = false,
    val isSearch: Boolean = false,
)

internal sealed interface EinkScreen {
    data object MessageList : EinkScreen
    data object Folders : EinkScreen
    data object Accounts : EinkScreen
    data object Search : EinkScreen
    data object Reader : EinkScreen
    data object Compose : EinkScreen
    data object Settings : EinkScreen
    data object FolderManager : EinkScreen
}

internal enum class AttachmentStatus { None, Downloading, Saved, Failed }

internal data class ReaderAttachment(
    val info: AttachmentViewInfo,
    val status: AttachmentStatus = AttachmentStatus.None,
) {
    val name: String get() = info.displayName ?: "attachment"
}

internal data class ReaderState(
    val item: MessageListItem,
    val index: Int,
    val total: Int,
    val bodyText: String? = null,
    val bodyAnnotated: AnnotatedString? = null,
    val bodyHtml: String? = null,
    val attachments: List<ReaderAttachment> = emptyList(),
    val showHtml: Boolean = false,
    val loading: Boolean = true,
    val error: String? = null,
    val replyToAddress: String? = null,
    val messageId: String? = null,
    val references: String? = null,
)

/** Prefilled state for the MMD compose screen. */
internal data class ComposeState(
    val accountUuid: String,
    val to: String = "",
    val subject: String = "",
    val body: String = "",
    val inReplyTo: String? = null,
    val references: String? = null,
    val sending: Boolean = false,
    val error: String? = null,
)

/**
 * MonoMail e-ink UI: single state holder on top of the existing mail engine.
 * Message lists come from [MessageListLiveDataFactory] (the same loader the
 * classic UI uses), actions go through [MessagingController].
 */
internal class EinkViewModel(
    private val context: Context,
    private val accountManager: LegacyAccountDtoManager,
    folderRepository: DisplayFolderRepository,
    private val messageListLiveDataFactory: MessageListLiveDataFactory,
    private val messagingController: MessagingController,
    private val messageViewInfoExtractorFactory: MessageViewInfoExtractorFactory,
    private val generalSettingsManager: GeneralSettingsManager,
) : ViewModel() {

    val generalSettings: StateFlow<GeneralSettings> = generalSettingsManager.getConfigFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, generalSettingsManager.getConfig())

    fun updateGeneralSettings(updater: (GeneralSettings) -> GeneralSettings) {
        // The store writes synchronously to disk; keep that off the main thread.
        viewModelScope.launch(Dispatchers.IO) {
            generalSettingsManager.update(updater)
        }
    }

    val accounts: StateFlow<List<LegacyAccountDto>> = accountManager.getAccountsFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, accountManager.getAccounts())

    val currentAccountUuid = MutableStateFlow<String?>(null)

    @Suppress("OPT_IN_USAGE")
    val folders: StateFlow<List<DisplayFolder>> = currentAccountUuid
        .flatMapLatest { uuid ->
            if (uuid == null) flowOf(emptyList()) else folderRepository.getDisplayFoldersFlow(uuid)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** All folders, including hidden ones, for the folder manager screen. */
    @Suppress("OPT_IN_USAGE")
    val allFolders: StateFlow<List<DisplayFolder>> = currentAccountUuid
        .flatMapLatest { uuid ->
            val account = account(uuid)
            if (account == null) {
                flowOf(emptyList())
            } else {
                folderRepository.getDisplayFoldersFlow(account, includeHiddenFolders = true)
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val currentFolder = MutableStateFlow<FolderSelection?>(null)
    val messages = MutableStateFlow<List<MessageListItem>>(emptyList())
    val listLoading = MutableStateFlow(true)
    val reader = MutableStateFlow<ReaderState?>(null)

    val backStack = MutableStateFlow<List<EinkScreen>>(listOf(EinkScreen.MessageList))

    private var listLiveData: MessageListLiveData? = null
    private val listObserver = Observer<MessageListInfo> { info ->
        messages.value = info.messageListItems
        listLoading.value = false
        // Keep reader position/total in sync when the list changes underneath.
        reader.update { state ->
            state?.let {
                val newIndex = info.messageListItems.indexOfFirst { m -> m.uniqueId == state.item.uniqueId }
                if (newIndex >= 0) it.copy(index = newIndex, total = info.messageListItems.size) else it
            }
        }
    }

    init {
        viewModelScope.launch {
            accounts.collect { list ->
                if (currentAccountUuid.value == null && list.isNotEmpty()) {
                    selectAccount(list.first().uuid)
                }
            }
        }
    }

    fun account(uuid: String?): LegacyAccountDto? = uuid?.let { accountManager.getAccount(it) }

    fun currentAccount(): LegacyAccountDto? = account(currentAccountUuid.value)

    fun selectAccount(uuid: String) {
        currentAccountUuid.value = uuid
        val account = account(uuid) ?: return
        openFolder(account.inboxFolderId, "Inbox")
    }

    fun openUnifiedInbox() {
        val search = SearchAccount.createUnifiedFoldersSearch("Unified inbox", "").relatedSearch
        currentFolder.value = FolderSelection(accountUuid = null, folderId = null, name = "Unified inbox", isUnified = true)
        startList(config(search))
    }

    fun openFolder(folderId: Long?, name: String) {
        val uuid = currentAccountUuid.value ?: return
        if (folderId == null) return
        currentFolder.value = FolderSelection(uuid, folderId, name)
        val search = LocalMessageSearch().apply {
            addAccountUuid(uuid)
            addAllowedFolder(folderId)
        }
        startList(config(search))
    }

    fun search(query: String) {
        val uuid = currentAccountUuid.value ?: return
        if (query.isBlank()) return
        val search = LocalMessageSearch().apply {
            isManualSearch = true
            addAccountUuid(uuid)
            or(SearchCondition(MessageSearchField.SENDER, SearchAttribute.CONTAINS, query))
            or(SearchCondition(MessageSearchField.SUBJECT, SearchAttribute.CONTAINS, query))
            or(SearchCondition(MessageSearchField.MESSAGE_CONTENTS, SearchAttribute.CONTAINS, query))
        }
        currentFolder.value = FolderSelection(uuid, null, "Search: $query", isSearch = true)
        startList(config(search))
    }

    private fun config(search: LocalMessageSearch) = MessageListConfig(
        search = search,
        showingThreadedList = false,
        sortType = SortType.SORT_DATE,
        sortAscending = false,
        sortDateAscending = false,
        activeMessage = null,
        sortOverrides = emptyMap(),
    )

    private fun startList(config: MessageListConfig) {
        listLiveData?.removeObserver(listObserver)
        listLoading.value = true
        messages.value = emptyList()
        listLiveData = messageListLiveDataFactory.create(viewModelScope, config)
            .also { it.observeForever(listObserver) }
    }

    // Navigation

    fun push(screen: EinkScreen) {
        backStack.update { it + screen }
    }

    /** Bottom navigation: switch the root tab, dropping any pushed screens. */
    fun selectTab(screen: EinkScreen) {
        reader.value = null
        backStack.value = listOf(screen)
    }

    /** Returns false when the stack is exhausted and the activity should finish. */
    fun pop(): Boolean {
        val stack = backStack.value
        if (stack.size <= 1) return false
        if (stack.last() == EinkScreen.Reader) reader.value = null
        if (stack.last() == EinkScreen.Compose) compose.value = null
        backStack.value = stack.dropLast(1)
        return true
    }

    // Reader

    fun isDraft(item: MessageListItem): Boolean {
        val account = account(item.messageReference.accountUuid) ?: return false
        return account.draftsFolderId != null && item.folderId == account.draftsFolderId
    }

    fun openMessage(index: Int) {
        val item = messages.value.getOrNull(index) ?: return
        reader.value = ReaderState(item = item, index = index, total = messages.value.size)
        if (backStack.value.last() != EinkScreen.Reader) push(EinkScreen.Reader)
        loadBody(item)
        if (!item.isRead) setRead(item, true)
    }

    fun openAdjacentMessage(delta: Int) {
        val state = reader.value ?: return
        openMessage(state.index + delta)
    }

    fun toggleHtmlView() {
        reader.update { it?.copy(showHtml = !it.showHtml) }
    }

    /** Loaded message backing the reader, keyed by the item's uniqueId. */
    private var readerMessage: Pair<Long, LocalMessage>? = null

    private fun loadBody(item: MessageListItem) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val account = account(item.messageReference.accountUuid) ?: error("Account not found")
                val message = messagingController.loadMessage(account, item.folderId, item.messageUid)
                val extractor = messageViewInfoExtractorFactory.create(
                    HtmlSettings(useDarkMode = false, useFixedWidthFont = false),
                )
                val info = extractor.extractMessageForView(message, null, false)
                val html = info.text.orEmpty()
                val (text, annotated) = htmlToStyledText(html)
                val attachments = info.attachments.orEmpty()
                    .filterNot { it.inlineAttachment }
                    .map { ReaderAttachment(it) }
                readerMessage = item.uniqueId to message
                val replyTo = message.replyTo?.firstOrNull()?.address
                    ?: message.from?.firstOrNull()?.address
                val references = (message.references.orEmpty().toList() + listOfNotNull(message.messageId))
                    .joinToString(" ")
                    .ifBlank { null }
                withContext(Dispatchers.Main) {
                    reader.update { state ->
                        if (state?.item?.uniqueId == item.uniqueId) {
                            state.copy(
                                bodyText = text,
                                bodyAnnotated = annotated,
                                bodyHtml = html,
                                attachments = attachments,
                                loading = false,
                                replyToAddress = replyTo,
                                messageId = message.messageId,
                                references = references,
                            )
                        } else {
                            state
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    reader.update { state ->
                        if (state?.item?.uniqueId == item.uniqueId) {
                            state.copy(loading = false, error = e.message ?: "Could not load message")
                        } else {
                            state
                        }
                    }
                }
            }
        }
    }

    /** Fetch the attachment if needed, then save it to the system Downloads folder. */
    fun downloadAttachment(index: Int) {
        val state = reader.value ?: return
        val attachment = state.attachments.getOrNull(index) ?: return
        if (attachment.status == AttachmentStatus.Downloading) return
        val account = account(state.item.messageReference.accountUuid) ?: return
        val message = readerMessage?.takeIf { it.first == state.item.uniqueId }?.second ?: return
        setAttachmentStatus(index, AttachmentStatus.Downloading)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (!attachment.info.isContentAvailable) {
                    fetchAttachment(account, message, attachment.info)
                }
                saveToDownloads(attachment.info)
                setAttachmentStatus(index, AttachmentStatus.Saved)
            } catch (e: Exception) {
                setAttachmentStatus(index, AttachmentStatus.Failed)
            }
        }
    }

    private fun setAttachmentStatus(index: Int, status: AttachmentStatus) {
        reader.update { state ->
            state?.copy(
                attachments = state.attachments.mapIndexed { i, attachment ->
                    if (i == index) attachment.copy(status = status) else attachment
                },
            )
        }
    }

    private suspend fun fetchAttachment(
        account: LegacyAccountDto,
        message: LocalMessage,
        info: AttachmentViewInfo,
    ) = suspendCancellableCoroutine { continuation ->
        messagingController.loadAttachment(
            account,
            message,
            info.part,
            object : SimpleMessagingListener() {
                override fun loadAttachmentFinished(account: LegacyAccountDto?, message: Message?, part: Part?) {
                    info.setContentAvailable()
                    if (continuation.isActive) continuation.resume(Unit)
                }

                override fun loadAttachmentFailed(
                    account: LegacyAccountDto?,
                    message: Message?,
                    part: Part?,
                    reason: String?,
                ) {
                    if (continuation.isActive) {
                        continuation.resumeWithException(IOException(reason ?: "Could not download attachment"))
                    }
                }
            },
        )
    }

    private fun saveToDownloads(info: AttachmentViewInfo) {
        val name = info.displayName ?: "attachment"
        val resolver = context.contentResolver
        val input = resolver.openInputStream(info.internalUri) ?: throw IOException("Attachment content missing")
        input.use { stream ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                    put(MediaStore.MediaColumns.MIME_TYPE, info.mimeType ?: "application/octet-stream")
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: throw IOException("Could not create download entry")
                resolver.openOutputStream(uri)?.use { output -> stream.copyTo(output) }
                    ?: throw IOException("Could not open download for writing")
            } else {
                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                dir.mkdirs()
                File(dir, name).outputStream().use { output -> stream.copyTo(output) }
            }
        }
    }

    // Actions (all MessagingController calls are async-safe)

    fun setRead(item: MessageListItem, read: Boolean) {
        val account = account(item.messageReference.accountUuid) ?: return
        messagingController.setFlag(account, listOf(item.databaseId), Flag.SEEN, read)
    }

    fun toggleStar(item: MessageListItem) {
        val account = account(item.messageReference.accountUuid) ?: return
        messagingController.setFlag(account, listOf(item.databaseId), Flag.FLAGGED, !item.isStarred)
    }

    fun canArchive(item: MessageListItem): Boolean =
        account(item.messageReference.accountUuid)?.archiveFolderId != null

    fun archive(item: MessageListItem) {
        messagingController.archiveMessages(listOf(item.messageReference))
        closeReaderIfShowing(item.messageReference)
    }

    fun delete(item: MessageListItem) {
        messagingController.deleteMessages(listOf(item.messageReference))
        closeReaderIfShowing(item.messageReference)
    }

    private fun closeReaderIfShowing(reference: MessageReference) {
        if (reader.value?.item?.messageReference == reference && backStack.value.last() == EinkScreen.Reader) {
            pop()
        }
    }

    val syncing = MutableStateFlow(false)

    private val syncListener = object : SimpleMessagingListener() {
        override fun synchronizeMailboxFinished(account: LegacyAccountDto, folderId: Long) {
            syncing.value = false
        }

        override fun synchronizeMailboxFailed(account: LegacyAccountDto, folderId: Long, message: String?) {
            syncing.value = false
        }

        override fun checkMailFinished(context: android.content.Context?, account: LegacyAccountDto?) {
            syncing.value = false
        }
    }

    fun sync() {
        val folder = currentFolder.value ?: return
        syncing.value = true
        if (folder.isUnified || folder.folderId == null) {
            accounts.value.forEach { messagingController.checkMail(it, true, true, true, syncListener) }
        } else {
            account(folder.accountUuid)?.let {
                messagingController.synchronizeMailbox(it, folder.folderId, true, syncListener)
            }
        }
        // Safety net in case no listener callback ever fires.
        viewModelScope.launch {
            kotlinx.coroutines.delay(SYNC_INDICATOR_MS)
            syncing.value = false
        }
    }

    // Compose

    val compose = MutableStateFlow<ComposeState?>(null)

    fun startCompose() {
        val uuid = currentAccountUuid.value ?: return
        compose.value = ComposeState(accountUuid = uuid)
        push(EinkScreen.Compose)
    }

    fun startReply() {
        val state = reader.value ?: return
        val uuid = state.item.messageReference.accountUuid
        val subject = state.item.subject.orEmpty()
        val quoted = state.bodyText.orEmpty()
            .lineSequence()
            .joinToString("\n") { "> $it" }
        val header = "On ${formatMessageDateLong(state.item.messageDate)}, " +
            "${state.item.displayName} wrote:"
        compose.value = ComposeState(
            accountUuid = uuid,
            to = state.replyToAddress ?: state.item.displayAddress?.address.orEmpty(),
            subject = if (subject.startsWith("Re:", ignoreCase = true)) subject else "Re: $subject",
            body = "\n\n$header\n$quoted",
            inReplyTo = state.messageId,
            references = state.references,
        )
        push(EinkScreen.Compose)
    }

    fun send(to: String, subject: String, body: String) {
        val state = compose.value ?: return
        val account = account(state.accountUuid) ?: return
        val addresses = Address.parse(to.trim())
        if (addresses.isEmpty()) {
            compose.value = state.copy(error = "Enter a valid recipient address.")
            return
        }
        val identity = account.identities.firstOrNull() ?: return
        compose.value = state.copy(sending = true, error = null)

        val builder = SimpleMessageBuilder.newInstance()
            .setSubject(subject.ifBlank { " " })
            .setSentDate(java.util.Date())
            .setHideTimeZone(true)
            .setTo(addresses.toList())
            .setCc(emptyList())
            .setBcc(emptyList())
            .setReplyTo(emptyArray())
            .setInReplyTo(state.inReplyTo)
            .setReferences(state.references)
            .setRequestReadReceipt(false)
            .setIdentity(identity)
            .setMessageFormat(SimpleMessageFormat.TEXT)
            .setText(body)
            .setAttachments(emptyList())
            .setInlineAttachments(emptyMap())
            .setSignature(if (identity.signatureUse) identity.signature.orEmpty() else "")
            .setQuoteStyle(QuoteStyle.PREFIX)
            .setQuotedTextMode(QuotedTextMode.NONE)
            .setQuotedText("")
            .setReplyAfterQuote(false)
            .setSignatureBeforeQuotedText(false)
            .setIdentityChanged(false)
            .setSignatureChanged(false)
            .setCursorPosition(0)
            .setMessageReference(null)
            .setDraft(false)
            .setIsPgpInlineEnabled(false)

        builder.buildAsync(object : MessageBuilder.Callback {
            override fun onMessageBuildSuccess(message: MimeMessage, isDraft: Boolean) {
                messagingController.sendMessage(account, message, null, null)
                compose.value = null
                if (backStack.value.last() == EinkScreen.Compose) pop()
            }

            override fun onMessageBuildCancel() {
                compose.update { it?.copy(sending = false) }
            }

            override fun onMessageBuildException(exception: MessagingException) {
                compose.update { it?.copy(sending = false, error = exception.message ?: "Could not build message") }
            }

            override fun onMessageBuildReturnPendingIntent(pendingIntent: android.app.PendingIntent, requestCode: Int) {
                compose.update { it?.copy(sending = false, error = "Interactive send is not supported here.") }
            }
        })
    }

    fun cancelCompose() {
        compose.value = null
        if (backStack.value.last() == EinkScreen.Compose) pop()
    }

    override fun onCleared() {
        listLiveData?.removeObserver(listObserver)
    }

    private companion object {
        const val SYNC_INDICATOR_MS = 15_000L

        /**
         * Parse email HTML into styled text. Style/script/head blocks are
         * dropped (fromHtml would print raw CSS), object-replacement chars
         * from images are removed ("OBJ" boxes), and bold/italic/underline
         * and relative sizes survive as an AnnotatedString.
         */
        fun htmlToStyledText(html: String): Pair<String, AnnotatedString> {
            val cleaned = html
                .replace(Regex("(?is)<head\\b[^>]*>.*?</head>"), "")
                .replace(Regex("(?is)<style\\b[^>]*>.*?</style>"), "")
                .replace(Regex("(?is)<script\\b[^>]*>.*?</script>"), "")
            val spanned = HtmlCompat.fromHtml(cleaned, HtmlCompat.FROM_HTML_MODE_LEGACY)

            val builder = SpannableStringBuilder(spanned)
            var i = builder.length - 1
            while (i >= 0) {
                when (builder[i]) {
                    '￼' -> builder.delete(i, i + 1)
                    ' ' -> builder.replace(i, i + 1, " ")
                }
                i--
            }
            // Collapse 3+ consecutive newlines while spans are still attached.
            var j = builder.length - 1
            var newlines = 0
            while (j >= 0) {
                if (builder[j] == '\n') {
                    newlines++
                    if (newlines > 2) builder.delete(j, j + 1)
                } else {
                    newlines = 0
                }
                j--
            }

            val plain = builder.toString().trim()
            val annotated = buildAnnotatedString {
                append(builder.toString())
                builder.getSpans(0, builder.length, Any::class.java).forEach { span ->
                    val start = builder.getSpanStart(span)
                    val end = builder.getSpanEnd(span)
                    if (start < 0 || end <= start) return@forEach
                    when (span) {
                        is StyleSpan -> when (span.style) {
                            Typeface.BOLD ->
                                addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, end)

                            Typeface.ITALIC ->
                                addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, end)

                            Typeface.BOLD_ITALIC -> addStyle(
                                SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic),
                                start,
                                end,
                            )
                        }

                        is UnderlineSpan ->
                            addStyle(SpanStyle(textDecoration = TextDecoration.Underline), start, end)

                        is URLSpan -> addStyle(
                            SpanStyle(
                                textDecoration = TextDecoration.Underline,
                                fontWeight = FontWeight.Medium,
                            ),
                            start,
                            end,
                        )

                        is RelativeSizeSpan -> addStyle(
                            SpanStyle(fontSize = (BODY_FONT_SIZE_SP * span.sizeChange).sp),
                            start,
                            end,
                        )
                    }
                }
            }
            return plain to annotated
        }

        private const val BODY_FONT_SIZE_SP = 16f
    }
}
