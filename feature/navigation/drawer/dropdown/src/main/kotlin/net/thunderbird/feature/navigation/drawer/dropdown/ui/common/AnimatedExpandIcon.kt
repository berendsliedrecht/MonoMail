package net.thunderbird.feature.navigation.drawer.dropdown.ui.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import net.thunderbird.components.ui.bolt.atom.icon.Icon
import net.thunderbird.components.ui.bolt.atom.icon.Icons

@Composable
internal fun AnimatedExpandIcon(
    isExpanded: Boolean,
    modifier: Modifier = Modifier,
    isShowAnimations: Boolean = true,
    tint: Color? = null,
) {
    // MonoMail: always snap; rotation animation ghosts on e-ink.
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = snap(),
        label = "rotationAngle",
    )

    Icon(
        imageVector = Icons.Outlined.KeyboardArrowDown,
        contentDescription = null,
        tint = tint,
        modifier = modifier
            .rotate(rotationAngle),
    )
}
