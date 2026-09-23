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

/**
 * MMD folder manager: every folder (including hidden ones); tapping opens the
 * classic per-folder settings (display class, sync, notifications).
 */
@Composable
internal fun FolderManagerScreen(
    viewModel: EinkViewModel,
    onOpenFolderSettings: (folderId: Long) -> Unit,
) {
    val folders by viewModel.allFolders.collectAsState()
    val account = viewModel.currentAccount()

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        EinkTopBar(
            title = "Manage folders",
            subtitle = account?.email,
        )

        if (folders.isEmpty()) {
            EmptyState("No folders.", modifier = Modifier.weight(1f))
        } else {
            LazyColumnMMD(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) {
                items(folders.size) { index ->
                    val folder = folders[index]
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .einkClickable { onOpenFolderSettings(folder.folder.id) }
                            .padding(top = 10.dp, bottom = 10.dp),
                    ) {
                        TextMMD(
                            text = folder.folder.name,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        TextMMD(
                            text = folder.folder.type.name.lowercase().replaceFirstChar { it.uppercase() },
                            fontSize = 16.sp,
                            color = Color.Black,
                            maxLines = 1,
                        )
                    }
                    if (index < folders.lastIndex) DashedDivider()
                }
            }
        }
    }
}
