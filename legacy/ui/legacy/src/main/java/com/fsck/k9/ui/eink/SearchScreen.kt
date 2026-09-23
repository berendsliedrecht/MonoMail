package com.fsck.k9.ui.eink

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.fsck.k9.ui.messagelist.MessageListItem
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import com.mudita.mmd.components.lazy.LazyColumnMMD
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.components.text_field.TextFieldMMD

@Composable
internal fun SearchScreen(
    viewModel: EinkViewModel,
    onOpenDraft: (MessageListItem) -> Unit,
) {
    val messages by viewModel.messages.collectAsState()
    val loading by viewModel.listLoading.collectAsState()
    val folder by viewModel.currentFolder.collectAsState()

    var query by rememberSaveable { mutableStateOf("") }
    var searched by rememberSaveable { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    val showResults = searched && folder?.isSearch == true

    fun submit() {
        keyboard?.hide()
        viewModel.search(query)
        searched = true
    }

    LaunchedEffect(Unit) {
        if (!searched) focusRequester.requestFocus()
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        // The search field is the header, like MonoMusic's search.
        TextFieldMMD(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            placeholder = { TextMMD("Search mail") },
            trailingIcon = {
                EinkIconButton(icon = SearchIcon, onClick = { submit() }, contentDescription = "Search")
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { submit() }),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .focusRequester(focusRequester),
        )
        HorizontalDividerMMD()

        when {
            !showResults -> EmptyState("Search sender, subject, and text.", modifier = Modifier.weight(1f))
            loading -> EmptyState("Searching...", modifier = Modifier.weight(1f))
            messages.isEmpty() -> EmptyState("No results.", modifier = Modifier.weight(1f))
            else -> LazyColumnMMD(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) {
                items(messages.size) { index ->
                    val item = messages[index]
                    MessageRow(
                        item = item,
                        onClick = {
                            if (viewModel.isDraft(item)) onOpenDraft(item) else viewModel.openMessage(index)
                        },
                        onLongClick = {},
                    )
                    if (index < messages.lastIndex) DashedDivider()
                }
            }
        }
    }
}
