package net.thunderbird.feature.mail.message.list.internal.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import com.mudita.mmd.components.text.TextMMD
import net.thunderbird.components.ui.bolt.atom.icon.Icon
import net.thunderbird.components.ui.bolt.atom.icon.Icons
import net.thunderbird.feature.mail.message.list.preferences.MessageListPreferences
import net.thunderbird.feature.mail.message.list.ui.state.MessageItemUi

/**
 * MonoMail: MMD-styled message row for e-ink. Pure black/white, no avatar,
 * no ripple, emphasis through weight instead of color. Selected rows invert.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun MonoMessageListItem(
    message: MessageItemUi,
    preferences: MessageListPreferences,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    onFavouriteClick: () -> Unit = {},
) {
    val inverted = message.selected || message.active
    val foreground = if (inverted) Color.White else Color.Black
    val background = if (inverted) Color.Black else Color.White
    val emphasis = if (message.state == MessageItemUi.State.Read) FontWeight.Normal else FontWeight.Bold

    Column(modifier = modifier.fillMaxWidth().background(background)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                    onLongClick = onLongClick,
                )
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextMMD(
                    text = message.senders.displayName,
                    fontSize = 16.sp,
                    fontWeight = emphasis,
                    color = foreground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(8.dp))
                TextMMD(
                    text = message.formattedReceivedAt,
                    fontSize = 13.sp,
                    color = foreground,
                    maxLines = 1,
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                TextMMD(
                    text = message.subject.ifBlank { "(no subject)" },
                    fontSize = 15.sp,
                    fontWeight = emphasis,
                    color = foreground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (preferences.groupConversations && message.threadCount > 1) {
                    Spacer(modifier = Modifier.width(8.dp))
                    TextMMD(
                        text = "(${message.threadCount})",
                        fontSize = 13.sp,
                        color = foreground,
                    )
                }
                if (message.hasAttachments) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Outlined.Attachment,
                        contentDescription = null,
                        tint = foreground,
                        modifier = Modifier.size(16.dp),
                    )
                }
                if (preferences.showFavouriteButton) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = if (message.starred) Icons.Filled.Star else Icons.Outlined.Star,
                        contentDescription = null,
                        tint = foreground,
                        modifier = Modifier
                            .size(20.dp)
                            .combinedClickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onFavouriteClick,
                            ),
                    )
                }
            }

            if (preferences.excerptLines > 0 && message.excerpt.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                TextMMD(
                    text = message.excerpt,
                    fontSize = 14.sp,
                    color = foreground,
                    maxLines = preferences.excerptLines,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        HorizontalDividerMMD()
    }
}
