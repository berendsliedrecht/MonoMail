package net.thunderbird.components.ui.bolt.theme.thunderbird

import androidx.compose.ui.graphics.Color
import net.thunderbird.components.ui.bolt.theme.ThemeColorScheme

// MonoMail: pure black/white palette for e-ink displays. Anything between
// black and white dithers on e-ink, so every role maps to one of the two.
internal val lightThemeColorScheme = ThemeColorScheme(
    primary = Color.Black,
    onPrimary = Color.White,
    primaryContainer = Color.Black,
    onPrimaryContainer = Color.White,

    secondary = Color.Black,
    onSecondary = Color.White,
    secondaryContainer = Color.Black,
    onSecondaryContainer = Color.White,

    tertiary = Color.Black,
    onTertiary = Color.White,
    tertiaryContainer = Color.Black,
    onTertiaryContainer = Color.White,

    error = Color.Black,
    onError = Color.White,
    errorContainer = Color.White,
    onErrorContainer = Color.Black,

    surfaceDim = Color.White,
    surface = Color.White,
    surfaceBright = Color.White,
    onSurface = Color.Black,
    onSurfaceVariant = Color.Black,

    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color.White,
    surfaceContainerHighest = Color.White,

    inverseSurface = Color.Black,
    inverseOnSurface = Color.White,
    inversePrimary = Color.White,

    outline = Color.Black,
    outlineVariant = Color.Black,

    scrim = Color.Black,

    info = Color.Black,
    onInfo = Color.White,
    infoContainer = Color.White,
    onInfoContainer = Color.Black,

    success = Color.Black,
    onSuccess = Color.White,
    successContainer = Color.White,
    onSuccessContainer = Color.Black,

    warning = Color.Black,
    onWarning = Color.White,
    warningContainer = Color.White,
    onWarningContainer = Color.Black,
)

// MonoMail: e-ink has no dark mode; the dark scheme mirrors the light one so
// a system dark setting cannot produce a black background that ghosts.
internal val darkThemeColorScheme = lightThemeColorScheme
