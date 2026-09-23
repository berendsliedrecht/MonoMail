package com.fsck.k9.ui.eink

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.fsck.k9.activity.compose.MessageActions
import com.fsck.k9.ui.managefolders.ManageFoldersActivity
import com.fsck.k9.ui.settings.account.AccountSettingsActivity
import com.fsck.k9.ui.settings.general.GeneralSettingsActivity
import net.thunderbird.core.android.account.LegacyAccountDto
import com.fsck.k9.ui.messagelist.MessageListItem
import com.fsck.k9.ui.settings.SettingsActivity
import com.mudita.mmd.ThemeMMD
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * MonoMail e-ink UI: single activity hosting the paged black/white screens.
 * Message lists, folders, accounts, search, and the reader are new; compose,
 * settings, and account setup reuse the existing (themed) activities.
 */
class EinkMailActivity : AppCompatActivity() {

    private val viewModel: EinkViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        intent.getStringExtra(EXTRA_ACCOUNT_UUID)?.let { uuid ->
            viewModel.selectAccount(uuid)
        }

        setContent {
            ThemeMMD {
                EinkApp(
                    viewModel = viewModel,
                    onFinish = { finish() },
                    onOpenDraft = { item -> MessageActions.actionEditDraft(this, item.messageReference) },
                    onReply = { item, replyAll ->
                        MessageActions.actionReply(this, item.messageReference, replyAll, null)
                    },
                    onForward = { item -> MessageActions.actionForward(this, item.messageReference, null) },
                    onOpenFullComposer = { MessageActions.actionCompose(this, viewModel.currentAccount()) },
                    onGeneralSettings = { GeneralSettingsActivity.start(this) },
                    onAccountSettings = { account -> AccountSettingsActivity.start(this, account.uuid) },
                    onAddAccount = { SettingsActivity.launch(this) },
                    onOpenFolderSettings = { folderId ->
                        viewModel.currentAccount()?.let {
                            ManageFoldersActivity.launchFolderSettings(this, it, folderId)
                        }
                    },
                )
            }
        }
    }

    companion object {
        private const val EXTRA_ACCOUNT_UUID = "account_uuid"

        @JvmStatic
        @JvmOverloads
        fun launch(context: Context, accountUuid: String? = null) {
            val intent = Intent(context, EinkMailActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                accountUuid?.let { putExtra(EXTRA_ACCOUNT_UUID, it) }
            }
            context.startActivity(intent)
        }
    }
}

@Composable
internal fun EinkApp(
    viewModel: EinkViewModel,
    onFinish: () -> Unit,
    onOpenDraft: (MessageListItem) -> Unit,
    onReply: (MessageListItem, Boolean) -> Unit,
    onForward: (MessageListItem) -> Unit,
    onOpenFullComposer: () -> Unit,
    onGeneralSettings: () -> Unit,
    onAccountSettings: (LegacyAccountDto) -> Unit,
    onAddAccount: () -> Unit,
    onOpenFolderSettings: (Long) -> Unit,
) {
    val backStack by viewModel.backStack.collectAsState()
    val current = backStack.last()

    BackHandler {
        if (!viewModel.pop()) onFinish()
    }

    // E-ink: the stretch overscroll effect deforms the whole screen and
    // ghosts badly; pulling past the edge must be a no-op.
    CompositionLocalProvider(LocalOverscrollFactory provides null) {
        when (current) {
            EinkScreen.MessageList -> MessageListScreen(
                viewModel = viewModel,
                onCompose = { viewModel.startCompose() },
                onOpenDraft = onOpenDraft,
            )

            EinkScreen.Folders -> FoldersScreen(viewModel = viewModel)

            EinkScreen.Accounts -> AccountsScreen(
                viewModel = viewModel,
                onAddAccount = onAddAccount,
                onAccountSettings = onAccountSettings,
            )

            EinkScreen.Search -> SearchScreen(
                viewModel = viewModel,
                onOpenDraft = onOpenDraft,
            )

            EinkScreen.Compose -> ComposeScreen(
                viewModel = viewModel,
                onOpenFullComposer = onOpenFullComposer,
            )

            EinkScreen.Settings -> SettingsScreen(
                viewModel = viewModel,
                onAdvancedSettings = onGeneralSettings,
            )

            EinkScreen.FolderManager -> FolderManagerScreen(
                viewModel = viewModel,
                onOpenFolderSettings = onOpenFolderSettings,
            )

            EinkScreen.Reader -> ReaderScreen(
                viewModel = viewModel,
                onReply = { replyAll -> viewModel.reader.value?.let { onReply(it.item, replyAll) } },
                onForward = { viewModel.reader.value?.let { onForward(it.item) } },
            )
        }
    }
}
