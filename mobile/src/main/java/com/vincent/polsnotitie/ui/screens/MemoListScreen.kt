package com.vincent.polsnotitie.ui.screens

import android.app.Activity
import android.content.Intent
import android.content.ActivityNotFoundException
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vincent.polsnotitie.MemoProcessor
import com.vincent.polsnotitie.R
import com.vincent.polsnotitie.data.Category
import com.vincent.polsnotitie.data.Memo
import com.vincent.polsnotitie.language.LanguagePreference
import com.vincent.polsnotitie.ui.common.displayName
import com.vincent.polsnotitie.ui.common.copyToClipboard
import com.vincent.polsnotitie.ui.common.rememberDateFormatter
import com.vincent.polsnotitie.ui.common.shareText
import com.vincent.polsnotitie.ui.theme.AmberGold
import com.vincent.polsnotitie.ui.theme.BalticBlue
import com.vincent.polsnotitie.ui.theme.InkDark
import com.vincent.polsnotitie.ui.theme.Platinum
import com.vincent.polsnotitie.ui.theme.SteelAzure
import com.vincent.polsnotitie.ui.theme.VibrantCoral
import com.vincent.polsnotitie.ui.viewmodel.MemoListViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoListScreen(
    onOpenShopping: () -> Unit,
    onOpenSettings: () -> Unit,
    onSetTime: (String) -> Unit,
    viewModel: MemoListViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val formatTimestamp = rememberDateFormatter()

    val micLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                ?.trim()
            if (!spoken.isNullOrEmpty()) {
                scope.launch(Dispatchers.IO) {
                    MemoProcessor(context).process(
                        UUID.randomUUID().toString(), spoken, System.currentTimeMillis()
                    )
                }
            }
        }
    }

    fun startMic() {
        val prefs = context.getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
        val locale = LanguagePreference.speechLocale(prefs)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale)
            putExtra(RecognizerIntent.EXTRA_PROMPT, context.getString(R.string.speech_prompt))
        }
        try {
            micLauncher.launch(intent)
        } catch (e: ActivityNotFoundException) {
        }
    }

    val query by viewModel.query.collectAsStateWithLifecycle()
    val memos by viewModel.memos.collectAsStateWithLifecycle()
    val placesById by viewModel.placesById.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    TextButton(onClick = onOpenShopping) {
                        Icon(
                            Icons.Filled.ShoppingCart,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Text(stringResource(R.string.shopping_label))
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings_label))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { viewModel.setQuery(it) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    placeholder = { Text(stringResource(R.string.search_hint)) }
                )
                IconButton(onClick = { startMic() }) {
                    Icon(Icons.Filled.Mic, contentDescription = stringResource(R.string.record_label))
                }
            }

            if (memos.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (query.isBlank()) stringResource(R.string.no_notes) else stringResource(R.string.no_results),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(memos, key = { it.id }) { memo ->
                        val placeName = memo.placeId?.toLongOrNull()
                            ?.let { placesById[it]?.name }
                        SwipeableMemoItem(
                            memo = memo,
                            placeName = placeName,
                            onDelete = { viewModel.delete(memo) },
                            onPin = { viewModel.togglePin(memo) },
                            onSetTime = onSetTime,
                            formatTimestamp = formatTimestamp
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableMemoItem(
    memo: Memo,
    placeName: String?,
    onDelete: () -> Unit,
    onPin: () -> Unit,
    onSetTime: (String) -> Unit,
    formatTimestamp: (Long) -> String
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value != SwipeToDismissBoxValue.Settled) {
                onDelete()
                true
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.delete_label),
                    tint = Color.White
                )
            }
        }
    ) {
        MemoCard(memo = memo, placeName = placeName, onPin = onPin, onSetTime = onSetTime, formatTimestamp = formatTimestamp)
    }
}

@Composable
private fun MemoCard(
    memo: Memo,
    placeName: String?,
    onPin: () -> Unit,
    onSetTime: (String) -> Unit,
    formatTimestamp: (Long) -> String
) {
    val context = LocalContext.current
    val category = Category.fromName(memo.category)
    val pinned = memo.pinnedAt != null
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (pinned) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            CategoryChip(category)
            Text(
                text = memo.text,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp)
            )
            if (category == Category.HERINNERINGEN) {
                if (placeName != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Place,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 6.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.at_place, placeName),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .clickable { onSetTime(memo.id) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Schedule,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 6.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = memo.remindAt?.let { stringResource(R.string.reminder_prefix, formatTimestamp(it)) }
                                ?: stringResource(R.string.add_time),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatTimestamp(memo.timestamp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onPin) {
                    Icon(
                        Icons.Filled.PushPin,
                        contentDescription = if (pinned) stringResource(R.string.unpin_label)
                                             else stringResource(R.string.pin_label),
                        tint = if (pinned) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { copyToClipboard(context, memo.text) }) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = stringResource(R.string.copy_label))
                }
                IconButton(onClick = { shareText(context, memo.text) }) {
                    Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.share_label))
                }
            }
        }
    }
}

@Composable
private fun CategoryChip(category: Category) {
    val color = categoryColor(category)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color)
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(
            text = category.displayName(LocalContext.current),
            style = MaterialTheme.typography.labelMedium,
            color = if (color.luminance() > 0.5f) InkDark else Color.White
        )
    }
}

private fun categoryColor(category: Category): Color = when (category) {
    Category.BOODSCHAPPEN -> VibrantCoral
    Category.TODO -> BalticBlue
    Category.IDEEEN -> AmberGold
    Category.HERINNERINGEN -> SteelAzure
    Category.AGENDA -> SteelAzure
    Category.OVERIG -> Platinum
}
