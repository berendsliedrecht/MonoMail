package com.fsck.k9.ui.eink

import androidx.compose.foundation.background
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mudita.mmd.components.lazy.LazyColumnMMD
import com.mudita.mmd.components.text.TextMMD
import net.thunderbird.components.ui.bolt.atom.icon.Icon
import net.thunderbird.components.ui.bolt.atom.icon.Icons

@Composable
internal fun AccountsScreen(
    viewModel: EinkViewModel,
    onAddAccount: () -> Unit,
    onAccountSettings: (net.thunderbird.core.android.account.LegacyAccountDto) -> Unit,
) {
    val accounts by viewModel.accounts.collectAsState()
    val currentUuid by viewModel.currentAccountUuid.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        EinkTopBar(title = "Accounts")

        LazyColumnMMD(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) {
            items(accounts.size) { index ->
                val account = accounts[index]
                val selected = account.uuid == currentUuid
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .einkClickable {
                            viewModel.selectAccount(account.uuid)
                            viewModel.selectTab(EinkScreen.MessageList)
                        }
                        .padding(top = 10.dp, bottom = 10.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        TextMMD(
                            text = account.displayName,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        TextMMD(
                            text = account.email,
                            fontSize = 16.sp,
                            color = Color.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (selected) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = "Current account",
                            tint = Color.Black,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    EinkIconButton(
                        icon = Icons.Outlined.Settings,
                        onClick = { onAccountSettings(account) },
                        contentDescription = "Account settings",
                    )
                }
                DashedDivider()
            }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .einkClickable(onAddAccount)
                        .padding(top = 10.dp, bottom = 10.dp),
                ) {
                    TextMMD(
                        text = "Add account",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                    )
                }
            }
        }
    }
}
