package net.thunderbird.feature.mail.message.list.internal.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import net.thunderbird.feature.mail.message.list.preferences.MessageListPreferences
import net.thunderbird.feature.mail.message.list.ui.state.MessageItemUi

// MonoMail: all states render through the MMD e-ink row (MonoMessageListItem);
// the upstream New/Read/Unread organisms are bypassed. showAccountIndicator and
// onAvatarClick are kept for call-site compatibility but unused (no avatars,
// no account colors on e-ink).
@Suppress("UnusedParameter")
@Composable
internal fun MessageListItem(
    message: MessageItemUi,
    showAccountIndicator: Boolean,
    preferences: MessageListPreferences,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    onAvatarClick: () -> Unit = {},
    onFavouriteClick: () -> Unit = {},
) {
    MonoMessageListItem(
        message = message,
        preferences = preferences,
        onClick = onClick,
        onLongClick = onLongClick,
        onFavouriteClick = onFavouriteClick,
        modifier = modifier.testTag(
            when (message.state) {
                MessageItemUi.State.New -> MessageListItemDefaults.NEW_MESSAGE_LIST_TEST_TAG
                MessageItemUi.State.Read -> MessageListItemDefaults.READ_MESSAGE_LIST_TEST_TAG
                MessageItemUi.State.Unread -> MessageListItemDefaults.UNREAD_MESSAGE_LIST_TEST_TAG
            },
        ),
    )
}

internal object MessageListItemDefaults {
    const val NEW_MESSAGE_LIST_TEST_TAG = "NewMessageListItem_Root"
    const val READ_MESSAGE_LIST_TEST_TAG = "ReadMessageListItem_Root"
    const val UNREAD_MESSAGE_LIST_TEST_TAG = "UnreadMessageListItem_Root"
}
