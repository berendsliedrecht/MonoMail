package com.fsck.k9.ui.eink

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.components.text_field.TextFieldMMD

/**
 * MMD compose screen for plain-text mail. Attachments, CC/BCC, encryption, and
 * drafts stay in the full composer, reachable from here.
 */
@Composable
internal fun ComposeScreen(
    viewModel: EinkViewModel,
    onOpenFullComposer: () -> Unit,
) {
    val state by viewModel.compose.collectAsState()
    val compose = state ?: return

    var to by rememberSaveable(compose.accountUuid) { mutableStateOf(compose.to) }
    var subject by rememberSaveable(compose.accountUuid) { mutableStateOf(compose.subject) }
    var body by rememberSaveable(compose.accountUuid) { mutableStateOf(compose.body) }

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        EinkTopBar(
            title = if (compose.inReplyTo != null) "Reply" else "New email",
            subtitle = viewModel.account(compose.accountUuid)?.email,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            TextFieldMMD(
                value = to,
                onValueChange = { to = it },
                singleLine = true,
                placeholder = { TextMMD("To") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    autoCorrectEnabled = false,
                    capitalization = KeyboardCapitalization.None,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(10.dp))
            TextFieldMMD(
                value = subject,
                onValueChange = { subject = it },
                singleLine = true,
                placeholder = { TextMMD("Subject") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(10.dp))
            TextFieldMMD(
                value = body,
                onValueChange = { body = it },
                placeholder = { TextMMD("Write your email") },
                minLines = 10,
                modifier = Modifier.fillMaxWidth(),
            )
            compose.error?.let { error ->
                Spacer(modifier = Modifier.height(8.dp))
                TextMMD(text = error, fontSize = 14.sp, color = Color.Black)
            }
            Spacer(modifier = Modifier.height(12.dp))
            TextMMD(
                text = "Attachments, CC, and encryption are in the full composer.",
                fontSize = 13.sp,
                color = Color.Black,
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedButtonMMD(
                onClick = onOpenFullComposer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                TextMMD(text = "Open full composer", fontSize = 15.sp)
            }
        }

        HorizontalDividerMMD()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            OutlinedButtonMMD(
                onClick = { viewModel.cancelCompose() },
                modifier = Modifier.weight(1f),
            ) {
                TextMMD(text = "Discard", fontSize = 15.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            ButtonMMD(
                onClick = { viewModel.send(to, subject, body) },
                enabled = !compose.sending && to.isNotBlank(),
                modifier = Modifier.weight(1f),
            ) {
                TextMMD(text = if (compose.sending) "Sending..." else "Send", fontSize = 15.sp)
            }
        }
    }
}
