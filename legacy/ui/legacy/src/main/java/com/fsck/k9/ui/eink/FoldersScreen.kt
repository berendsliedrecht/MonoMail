package com.fsck.k9.ui.eink

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mudita.mmd.components.lazy.LazyColumnMMD
import com.mudita.mmd.components.text.TextMMD

@Composable
internal fun FoldersScreen(
    viewModel: EinkViewModel,
) {
    val folders by viewModel.folders.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val account = viewModel.currentAccount()

    val showUnified = accounts.size > 1

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        EinkTopBar(
            title = "Folders",
            subtitle = account?.email,
        )

        if (folders.isEmpty()) {
            EmptyState("No folders.", modifier = Modifier.weight(1f))
        } else {
            LazyColumnMMD(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) {
                if (showUnified) {
                    item {
                        FolderRow(name = "Unified inbox", subtitle = "All accounts") {
                            viewModel.openUnifiedInbox()
                            viewModel.selectTab(EinkScreen.MessageList)
                        }
                        DashedDivider()
                    }
                }
                items(folders.size) { index ->
                    val folder = folders[index]
                    FolderRow(
                        name = folder.folder.name,
                        subtitle = folder.unreadMessageCount
                            .takeIf { it > 0 }
                            ?.let { "$it unread" },
                    ) {
                        viewModel.openFolder(folder.folder.id, folder.folder.name)
                        viewModel.selectTab(EinkScreen.MessageList)
                    }
                    if (index < folders.lastIndex) DashedDivider()
                }
            }
        }
    }
}

@Composable
private fun FolderRow(
    name: String,
    subtitle: String?,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .einkClickable(onClick)
            .padding(top = 10.dp, bottom = 10.dp),
    ) {
        TextMMD(
            text = name,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(4.dp))
            TextMMD(
                text = subtitle,
                fontSize = 16.sp,
                color = Color.Black,
                maxLines = 1,
            )
        }
    }
}
