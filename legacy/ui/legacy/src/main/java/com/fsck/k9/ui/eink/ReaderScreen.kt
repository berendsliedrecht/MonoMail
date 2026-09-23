package com.fsck.k9.ui.eink

import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.mudita.mmd.components.bottom_sheet.ModalBottomSheetMMD
import com.mudita.mmd.components.bottom_sheet.rememberModalBottomSheetMMDState
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import com.mudita.mmd.components.text.TextMMD
import net.thunderbird.components.ui.bolt.atom.icon.Icon
import net.thunderbird.components.ui.bolt.atom.icon.Icons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReaderScreen(
    viewModel: EinkViewModel,
    onReply: (replyAll: Boolean) -> Unit,
    onForward: () -> Unit,
) {
    val state by viewModel.reader.collectAsState()
    val reader = state ?: return

    var showSheet by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        EinkTopBar(
            title = reader.item.subject?.takeIf { it.isNotBlank() } ?: "(no subject)",
        ) {
            EinkIconButton(icon = Icons.Outlined.MoreVert, onClick = { showSheet = true })
        }

        if (reader.showHtml && reader.bodyHtml != null) {
            Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            settings.loadWithOverviewMode = true
                            settings.useWideViewPort = true
                            settings.builtInZoomControls = true
                            settings.displayZoomControls = false
                        }
                    },
                    update = { webView ->
                        webView.loadDataWithBaseURL(null, reader.bodyHtml, "text/html", "utf-8", null)
                    },
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
                AttachmentSection(reader, viewModel)
            }
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                ReaderHeader(reader)
                HorizontalDividerMMD()
                run {
                    when {
                        reader.loading -> TextMMD(
                            text = "Loading...",
                            fontSize = 15.sp,
                            color = Color.Black,
                            modifier = Modifier.padding(16.dp),
                        )

                        reader.error != null -> TextMMD(
                            text = reader.error,
                            fontSize = 15.sp,
                            color = Color.Black,
                            modifier = Modifier.padding(16.dp),
                        )

                        reader.bodyAnnotated != null && reader.bodyText?.isNotBlank() == true -> Text(
                            text = reader.bodyAnnotated,
                            fontSize = 16.sp,
                            lineHeight = 24.sp,
                            color = Color.Black,
                            modifier = Modifier.padding(16.dp),
                        )

                        else -> TextMMD(
                            text = reader.bodyText?.takeIf { it.isNotBlank() } ?: "(empty message)",
                            fontSize = 16.sp,
                            color = Color.Black,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
                AttachmentSection(reader, viewModel)
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        HorizontalDividerMMD()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            ButtonMMD(
                onClick = { viewModel.startReply() },
                modifier = Modifier.weight(1f),
            ) {
                TextMMD(text = "Reply", fontSize = 15.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            OutlinedButtonMMD(
                onClick = { showSheet = true },
                modifier = Modifier.weight(1f),
            ) {
                TextMMD(text = "More", fontSize = 15.sp)
            }
        }
    }

    if (showSheet) {
        ModalBottomSheetMMD(
            onDismissRequest = { showSheet = false },
            sheetState = rememberModalBottomSheetMMDState(skipPartiallyExpanded = true),
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                SheetAction("Reply all") {
                    showSheet = false
                    onReply(true)
                }
                SheetAction("Forward") {
                    showSheet = false
                    onForward()
                }
                SheetAction(if (reader.item.isStarred) "Remove star" else "Star") {
                    viewModel.toggleStar(reader.item)
                    showSheet = false
                }
                SheetAction("Mark unread") {
                    viewModel.setRead(reader.item, false)
                    showSheet = false
                    viewModel.pop()
                }
                if (viewModel.canArchive(reader.item)) {
                    SheetAction("Archive") {
                        showSheet = false
                        viewModel.archive(reader.item)
                    }
                }
                SheetAction("Delete") {
                    showSheet = false
                    viewModel.delete(reader.item)
                }
                SheetAction(if (reader.showHtml) "Text view" else "View original") {
                    viewModel.toggleHtmlView()
                    showSheet = false
                }
            }
        }
    }
}

/** Attachment rows; a tap saves the attachment to the system Downloads folder. */
@Composable
private fun AttachmentSection(reader: ReaderState, viewModel: EinkViewModel) {
    if (reader.attachments.isEmpty()) return
    HorizontalDividerMMD()
    reader.attachments.forEachIndexed { index, attachment ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .einkClickable { viewModel.downloadAttachment(index) }
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Attachment,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                TextMMD(
                    text = attachment.name,
                    fontSize = 14.sp,
                    color = Color.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val status = when (attachment.status) {
                    AttachmentStatus.None -> null
                    AttachmentStatus.Downloading -> "Downloading..."
                    AttachmentStatus.Saved -> "Saved to Downloads"
                    AttachmentStatus.Failed -> "Could not save attachment"
                }
                if (status != null) {
                    TextMMD(text = status, fontSize = 12.sp, color = Color.Black)
                }
            }
        }
    }
    TextMMD(
        text = "Tap an attachment to save it to Downloads.",
        fontSize = 12.sp,
        color = Color.Black,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    )
}

@Composable
private fun ReaderHeader(reader: ReaderState) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        TextMMD(
            text = reader.item.displayName.toString(),
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        reader.item.displayAddress?.address?.let { address ->
            TextMMD(
                text = address,
                fontSize = 13.sp,
                color = Color.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.size(6.dp))
        TextMMD(
            text = reader.item.subject?.takeIf { it.isNotBlank() } ?: "(no subject)",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
        )
        Spacer(modifier = Modifier.size(2.dp))
        TextMMD(
            text = formatMessageDateLong(reader.item.messageDate),
            fontSize = 13.sp,
            color = Color.Black,
        )
    }
}
