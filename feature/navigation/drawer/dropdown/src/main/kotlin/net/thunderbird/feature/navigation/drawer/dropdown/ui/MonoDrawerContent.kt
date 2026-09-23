package net.thunderbird.feature.navigation.drawer.dropdown.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import com.mudita.mmd.components.text.TextMMD
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.DisplayAccount
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.MailDisplayAccount
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.UnifiedDisplayAccount
import net.thunderbird.feature.navigation.drawer.dropdown.ui.DrawerContract.Event
import net.thunderbird.feature.navigation.drawer.dropdown.ui.DrawerContract.State
import net.thunderbird.feature.navigation.drawer.dropdown.ui.account.AccountList
import net.thunderbird.feature.navigation.drawer.dropdown.ui.common.AnimatedExpandIcon
import net.thunderbird.feature.navigation.drawer.dropdown.ui.common.DRAWER_WIDTH
import net.thunderbird.feature.navigation.drawer.dropdown.ui.common.getAdditionalWidth
import net.thunderbird.feature.navigation.drawer.dropdown.ui.common.getDisplayAccountName
import net.thunderbird.feature.navigation.drawer.dropdown.ui.folder.FolderList
import net.thunderbird.feature.navigation.drawer.dropdown.ui.setting.AccountSettingList
import net.thunderbird.feature.navigation.drawer.dropdown.ui.setting.FolderSettingList

/**
 * MonoMail: MMD-styled drawer for e-ink. Replaces [DrawerContent]: no slide
 * animation between folder and account views, MMD typography in the header,
 * pure white surface. Folder tree, account list, and setting lists are reused.
 */
@Composable
internal fun MonoDrawerContent(
    state: State,
    onEvent: (Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedAccount = state.accounts.firstOrNull { it.id == state.selectedAccountId }

    Row(modifier = modifier.width(DRAWER_WIDTH + getAdditionalWidth()).fillMaxHeight()) {
        MonoDrawerColumn(state, selectedAccount, onEvent, modifier = Modifier.weight(1f))
        // Hard edge instead of a scrim: a scrim dithers on e-ink.
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(2.dp)
                .background(Color.Black),
        )
    }
}

@Composable
private fun MonoDrawerColumn(
    state: State,
    selectedAccount: DisplayAccount?,
    onEvent: (Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(Color.White)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag("DrawerContent"),
    ) {
        selectedAccount?.let { account ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onEvent(Event.OnAccountSelectorClick) }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    TextMMD(
                        text = getDisplayAccountName(account),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (account is MailDisplayAccount && account.name != account.email) {
                        TextMMD(
                            text = account.email,
                            fontSize = 13.sp,
                            color = Color.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                AnimatedExpandIcon(
                    isExpanded = state.showAccountSelection,
                    tint = Color.Black,
                )
            }
            HorizontalDividerMMD()
        }

        if (state.showAccountSelection) {
            Column(modifier = Modifier.fillMaxSize()) {
                AccountList(
                    accounts = state.accounts,
                    selectedAccount = selectedAccount,
                    onAccountClick = { onEvent(Event.OnAccountClick(it)) },
                    showStarredCount = state.config.showStarredCount,
                    modifier = Modifier.weight(1f),
                )
                HorizontalDividerMMD()
                AccountSettingList(
                    onAddAccountClick = { onEvent(Event.OnAddAccountClick) },
                    onSyncAllAccountsClick = { onEvent(Event.OnSyncAllAccounts) },
                    onSettingsClick = { onEvent(Event.OnSettingsClick) },
                    isLoading = state.isLoading,
                )
            }
        } else {
            val isUnifiedAccount = selectedAccount is UnifiedDisplayAccount

            Column(modifier = Modifier.fillMaxSize()) {
                FolderList(
                    isExpandedInitial = state.config.expandAllFolder,
                    rootFolder = state.rootFolder,
                    selectedFolder = state.selectedFolder,
                    onFolderClick = { folder -> onEvent(Event.OnFolderClick(folder)) },
                    showStarredCount = state.config.showStarredCount,
                    modifier = Modifier.weight(1f),
                )
                HorizontalDividerMMD()
                FolderSettingList(
                    onSyncAccountClick = { onEvent(Event.OnSyncAccount) },
                    onManageFoldersClick = { onEvent(Event.OnManageFoldersClick) },
                    onSyncAllAccountsClick = { onEvent(Event.OnSyncAllAccounts) },
                    onSettingsClick = { onEvent(Event.OnSettingsClick) },
                    isUnifiedAccount = isUnifiedAccount,
                    isLoading = state.isLoading,
                )
            }
        }
    }
}
