package com.fsck.k9.ui.eink

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.fsck.k9.ui.messagelist.MessageListItem
import com.mudita.mmd.components.bottom_sheet.ModalBottomSheetMMD
import com.mudita.mmd.components.bottom_sheet.rememberModalBottomSheetMMDState
import com.mudita.mmd.components.buttons.FloatingActionButtonMMD
import com.mudita.mmd.components.lazy.LazyColumnMMD
import com.mudita.mmd.components.text.TextMMD
import net.thunderbird.components.ui.bolt.atom.icon.Icon
import net.thunderbird.components.ui.bolt.atom.icon.Icons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MessageListScreen(
    viewModel: EinkViewModel,
    onCompose: () -> Unit,
    onOpenDraft: (MessageListItem) -> Unit,
) {
    val folder by viewModel.currentFolder.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val loading by viewModel.listLoading.collectAsState()
    val syncing by viewModel.syncing.collectAsState()
    val folders by viewModel.folders.collectAsState()

    var sheetItem by remember { mutableStateOf<MessageListItem?>(null) }
    var showMenu by remember { mutableStateOf(false) }

    val unread = folder?.folderId?.let { id ->
        folders.firstOrNull { it.folder.id == id }?.unreadMessageCount
    } ?: 0

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        EinkTopBar(
            title = folder?.name ?: "MonoMail",
            subtitle = when {
                syncing -> "Syncing..."
                unread > 0 -> "$unread unread"
                else -> null
            },
        ) {
            EinkIconButton(icon = Icons.Outlined.Sync, onClick = { viewModel.sync() }, contentDescription = "Sync")
            EinkIconButton(icon = SearchIcon, onClick = { viewModel.push(EinkScreen.Search) }, contentDescription = "Search")
            EinkIconButton(icon = Icons.Outlined.Menu, onClick = { showMenu = true }, contentDescription = "Menu")
        }

        Box(modifier = Modifier.weight(1f)) {
            when {
                loading -> EmptyState("Loading...", modifier = Modifier.fillMaxSize())
                messages.isEmpty() -> EmptyState("No messages.", modifier = Modifier.fillMaxSize())
                else -> LazyColumnMMD(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(messages.size) { index ->
                        val item = messages[index]
                        MessageRow(
                            item = item,
                            onClick = {
                                if (viewModel.isDraft(item)) onOpenDraft(item) else viewModel.openMessage(index)
                            },
                            onLongClick = { sheetItem = item },
                        )
                        if (index < messages.lastIndex) DashedDivider()
                    }
                }
            }
            FloatingActionButtonMMD(
                onClick = onCompose,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
            ) {
                Icon(imageVector = Icons.Outlined.Add, contentDescription = "Compose")
            }
        }
    }

    if (showMenu) {
        ModalBottomSheetMMD(
            onDismissRequest = { showMenu = false },
            sheetState = rememberModalBottomSheetMMDState(skipPartiallyExpanded = true),
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                SheetAction("Folders") {
                    showMenu = false
                    viewModel.push(EinkScreen.Folders)
                }
                SheetAction("Accounts") {
                    showMenu = false
                    viewModel.push(EinkScreen.Accounts)
                }
                SheetAction("Manage folders") {
                    showMenu = false
                    viewModel.push(EinkScreen.FolderManager)
                }
                SheetAction("Settings") {
                    showMenu = false
                    viewModel.push(EinkScreen.Settings)
                }
            }
        }
    }

    sheetItem?.let { item ->
        ModalBottomSheetMMD(
            onDismissRequest = { sheetItem = null },
            sheetState = rememberModalBottomSheetMMDState(skipPartiallyExpanded = true),
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                SheetAction(if (item.isRead) "Mark unread" else "Mark read") {
                    viewModel.setRead(item, !item.isRead)
                    sheetItem = null
                }
                SheetAction(if (item.isStarred) "Remove star" else "Star") {
                    viewModel.toggleStar(item)
                    sheetItem = null
                }
                if (viewModel.canArchive(item)) {
                    SheetAction("Archive") {
                        viewModel.archive(item)
                        sheetItem = null
                    }
                }
                SheetAction("Delete") {
                    viewModel.delete(item)
                    sheetItem = null
                }
            }
        }
    }
}

/** MonoMusic-style row: bold 20sp title line, 16sp subtitle line, no avatar. */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
internal fun MessageRow(
    item: MessageListItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(top = 10.dp, bottom = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextMMD(
                text = item.displayName.toString().ifEmpty { item.displayAddress?.address ?: "(unknown)" },
                fontSize = 20.sp,
                fontWeight = if (item.isRead) FontWeight.Bold else FontWeight.Black,
                color = Color.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(8.dp))
            if (!item.isRead) {
                Icon(
                    imageVector = Icons.Filled.Dot,
                    contentDescription = "Unread",
                    tint = Color.Black,
                    modifier = Modifier.size(12.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            TextMMD(
                text = formatMessageDate(item.messageDate),
                fontSize = 14.sp,
                color = Color.Black,
                maxLines = 1,
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (item.hasAttachments) {
                Icon(
                    imageVector = Icons.Outlined.Attachment,
                    contentDescription = "Attachment",
                    tint = Color.Black,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            if (item.isStarred) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = "Starred",
                    tint = Color.Black,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            TextMMD(
                text = item.subject?.takeIf { it.isNotBlank() } ?: "(no subject)",
                fontSize = 16.sp,
                color = Color.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
