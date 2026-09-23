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
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import com.mudita.mmd.components.lazy.LazyColumnMMD
import com.mudita.mmd.components.switcher.SwitchMMD
import com.mudita.mmd.components.text.TextMMD

/**
 * MMD general settings. A curated set of live toggles written straight to the
 * real settings store; everything else stays in the classic screen behind
 * "Advanced settings".
 */
@Composable
internal fun SettingsScreen(
    viewModel: EinkViewModel,
    onAdvancedSettings: () -> Unit,
) {
    val settings by viewModel.generalSettings.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        EinkTopBar(title = "Settings")

        LazyColumnMMD(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) {
            item { SectionHeader("Privacy") }
            item {
                ToggleRow(
                    title = "Hide time zone",
                    subtitle = "Use UTC in sent mail headers",
                    checked = settings.privacy.isHideTimeZone,
                ) { value ->
                    viewModel.updateGeneralSettings {
                        it.copy(privacy = it.privacy.copy(isHideTimeZone = value))
                    }
                }
            }
            item {
                ToggleRow(
                    title = "Hide app identity",
                    subtitle = "No user agent in sent mail",
                    checked = settings.privacy.isHideUserAgent,
                ) { value ->
                    viewModel.updateGeneralSettings {
                        it.copy(privacy = it.privacy.copy(isHideUserAgent = value))
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(16.dp))
                TextMMD(
                    text = "Languages, quiet time hours, and everything else:",
                    fontSize = 13.sp,
                    color = Color.Black,
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedButtonMMD(
                    onClick = onAdvancedSettings,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    TextMMD(text = "Advanced settings", fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
        TextMMD(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
        )
        HorizontalDividerMMD(modifier = Modifier.padding(vertical = 8.dp))
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    // Optimistic: the settings store persists to disk before its flow
    // re-emits, which reads as a dead switch. Flip locally right away and let
    // the store catch up (the remember key resyncs if it disagrees).
    var localChecked by remember(checked) { mutableStateOf(checked) }
    val toggle: (Boolean) -> Unit = { value ->
        localChecked = value
        onCheckedChange(value)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .einkClickable { toggle(!localChecked) }
            .padding(vertical = 8.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            TextMMD(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black,
            )
            TextMMD(
                text = subtitle,
                fontSize = 13.sp,
                color = Color.Black,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        SwitchMMD(
            checked = localChecked,
            onCheckedChange = toggle,
        )
    }
}
