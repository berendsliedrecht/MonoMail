package com.fsck.k9.ui.eink

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import com.mudita.mmd.components.text.TextMMD
import net.thunderbird.components.ui.bolt.atom.icon.Icon

/** Shared building blocks for the MonoMail e-ink UI, in the Mono app idiom. */

internal fun Modifier.einkClickable(onClick: () -> Unit): Modifier = composed {
    clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onClick,
    )
}

/** MonoMusic-style dashed divider between list rows. */
@Composable
internal fun DashedDivider(
    modifier: Modifier = Modifier,
    color: Color = Color.Black,
    thickness: Float = 2f,
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(2.dp),
    ) {
        drawLine(
            color = color,
            start = Offset(0f, size.height / 2),
            end = Offset(size.width, size.height / 2),
            strokeWidth = thickness,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 2.dp.toPx()), 0f),
        )
    }
}

/**
 * Top bar in the Mono app idiom: large bold title, actions on the right, solid
 * divider below. Hand-built because MMD 1.0.0's TopAppBarMMD crashes against
 * this repo's newer Material3 (invalid ColorSpace id at draw time).
 */
@Composable
internal fun EinkTopBar(
    title: String,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Column(modifier = Modifier.background(Color.White)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .padding(horizontal = 20.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                TextMMD(
                    text = title,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle != null) {
                    TextMMD(
                        text = subtitle,
                        fontSize = 13.sp,
                        color = Color.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            actions()
        }
        HorizontalDividerMMD()
    }
}

@Composable
internal fun EinkIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    contentDescription: String? = null,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(48.dp)
            .einkClickable(onClick),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.Black,
            modifier = Modifier.size(26.dp),
        )
    }
}

/** A full-width action row inside an MMD bottom sheet. */
@Composable
internal fun SheetAction(
    label: String,
    onClick: () -> Unit,
) {
    TextMMD(
        text = label,
        fontSize = 17.sp,
        color = Color.Black,
        modifier = Modifier
            .fillMaxWidth()
            .einkClickable(onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
    )
}

@Composable
internal fun EmptyState(text: String, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxWidth(),
    ) {
        TextMMD(text = text, fontSize = 16.sp, color = Color.Black)
    }
}

// Bolt has no magnifier icon; standard material "search" path, built once.
private var searchIconCache: ImageVector? = null
internal val SearchIcon: ImageVector
    get() = searchIconCache ?: ImageVector.Builder(
        name = "EinkSearch",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        addPath(
            pathData = addPathNodes(
                "M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 " +
                    "3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 " +
                    "0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z",
            ),
            fill = SolidColor(Color.Black),
        )
    }.build().also { searchIconCache = it }

/** Short date for list rows: time today, weekday within a week, date otherwise. */
internal fun formatMessageDate(epochMillis: Long): String {
    val zone = java.time.ZoneId.systemDefault()
    val dateTime = java.time.Instant.ofEpochMilli(epochMillis).atZone(zone)
    val today = java.time.LocalDate.now(zone)
    val date = dateTime.toLocalDate()
    val pattern = when {
        date == today -> "HH:mm"
        date.isAfter(today.minusDays(7)) -> "EEE"
        date.year == today.year -> "d MMM"
        else -> "d MMM yyyy"
    }
    return dateTime.format(java.time.format.DateTimeFormatter.ofPattern(pattern))
}

/** Full date for the reader header. */
internal fun formatMessageDateLong(epochMillis: Long): String {
    val zone = java.time.ZoneId.systemDefault()
    val dateTime = java.time.Instant.ofEpochMilli(epochMillis).atZone(zone)
    return dateTime.format(java.time.format.DateTimeFormatter.ofPattern("EEE d MMM yyyy, HH:mm"))
}
