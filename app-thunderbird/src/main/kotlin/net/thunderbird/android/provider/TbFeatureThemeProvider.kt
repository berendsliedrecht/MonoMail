package net.thunderbird.android.provider

import androidx.compose.runtime.Composable
import com.mudita.mmd.ThemeMMD
import net.thunderbird.components.ui.bolt.theme.thunderbird.ThunderbirdBoltTheme
import net.thunderbird.core.ui.theme.api.FeatureThemeProvider

// MonoMail: ThemeMMD wraps the Bolt theme so MMD components resolve their theme
// anywhere Compose feature content is rendered. Bolt-based components keep
// working through the inner ThunderbirdBoltTheme (black/white palette).
internal class TbFeatureThemeProvider : FeatureThemeProvider {
    @Composable
    override fun WithTheme(content: @Composable () -> Unit) {
        ThemeMMD {
            ThunderbirdBoltTheme {
                content()
            }
        }
    }

    @Composable
    override fun WithTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
        ThemeMMD {
            ThunderbirdBoltTheme(darkTheme = darkTheme) {
                content()
            }
        }
    }
}
