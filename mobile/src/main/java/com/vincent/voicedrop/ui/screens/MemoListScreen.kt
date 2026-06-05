package com.vincent.voicedrop.ui.screens

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.vincent.voicedrop.MemoProcessor
import com.vincent.voicedrop.R
import com.vincent.voicedrop.data.Category
import com.vincent.voicedrop.data.Memo
import com.vincent.voicedrop.language.LanguagePreference
import com.vincent.voicedrop.ui.common.copyToClipboard
import com.vincent.voicedrop.ui.common.displayName
import com.vincent.voicedrop.ui.common.rememberDateFormatter
import com.vincent.voicedrop.ui.common.shareText
import com.vincent.voicedrop.ui.theme.AmberGold
import com.vincent.voicedrop.ui.theme.InkDark
import com.vincent.voicedrop.ui.theme.Platinum
import com.vincent.voicedrop.ui.theme.SteelAzure
import com.vincent.voicedrop.ui.theme.Teal
import com.vincent.voicedrop.ui.theme.VibrantCoral
import com.vincent.voicedrop.ui.viewmodel.MemoListViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

private enum class HomeTab { NOTES, TASKS, SHOPPING }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    startInShopping: Boolean,
    onOpenSettings: () -> Unit,
    onSetTime: (String) -> Unit,
    viewModel: MemoListViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var tab by rememberSaveable {
        mutableStateOf(if (startInShopping) HomeTab.SHOPPING else HomeTab.NOTES)
    }
    var showTypeDialog by remember { mutableStateOf(false) }
    var typedText by remember { mutableStateOf("") }

    // Verwerkt een notitie via dezelfde pijplijn als spraak (classificeren + opslaan).
    fun processInput(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        scope.launch(Dispatchers.IO) {
            MemoProcessor(context).process(
                UUID.randomUUID().toString(), trimmed, System.currentTimeMillis()
            )
        }
    }

    val micLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spoken.isNullOrBlank()) processInput(spoken)
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

    val titleRes = when (tab) {
        HomeTab.NOTES -> R.string.nav_notes
        HomeTab.TASKS -> R.string.nav_tasks
        HomeTab.SHOPPING -> R.string.shopping_label
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(titleRes)) },
                actions = {
                    IconButton(onClick = { typedText = ""; showTypeDialog = true }) {
                        Icon(
                            Icons.Filled.Keyboard,
                            contentDescription = stringResource(R.string.type_label)
                        )
                    }
                    IconButton(onClick = { startMic() }) {
                        Icon(
                            Icons.Filled.Mic,
                            contentDescription = stringResource(R.string.record_label)
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings_label))
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == HomeTab.NOTES,
                    onClick = { tab = HomeTab.NOTES },
                    icon = { Icon(Icons.Filled.Description, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_notes)) }
                )
                NavigationBarItem(
                    selected = tab == HomeTab.TASKS,
                    onClick = { tab = HomeTab.TASKS },
                    icon = { Icon(Icons.Filled.CheckCircle, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_tasks)) }
                )
                NavigationBarItem(
                    selected = tab == HomeTab.SHOPPING,
                    onClick = { tab = HomeTab.SHOPPING },
                    icon = { Icon(Icons.Filled.ShoppingCart, contentDescription = null) },
                    label = { Text(stringResource(R.string.shopping_label)) }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(padding)) {
            when (tab) {
                HomeTab.NOTES -> NotesTab(viewModel, onSetTime)
                HomeTab.TASKS -> TasksTab(viewModel, onSetTime)
                HomeTab.SHOPPING -> ShoppingContent()
            }
        }
    }

    if (showTypeDialog) {
        AlertDialog(
            onDismissRequest = { showTypeDialog = false },
            title = { Text(stringResource(R.string.type_prompt)) },
            text = {
                OutlinedTextField(
                    value = typedText,
                    onValueChange = { typedText = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.type_prompt)) }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { processInput(typedText); showTypeDialog = false },
                    enabled = typedText.isNotBlank()
                ) { Text(stringResource(R.string.type_save)) }
            },
            dismissButton = {
                TextButton(onClick = { showTypeDialog = false }) {
                    Text(stringResource(R.string.cancel_label))
                }
            }
        )
    }
}

@Composable
private fun NotesTab(viewModel: MemoListViewModel, onSetTime: (String) -> Unit) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val formatTimestamp = rememberDateFormatter()

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { viewModel.setQuery(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            singleLine = true,
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            placeholder = { Text(stringResource(R.string.search_hint)) }
        )
        if (notes.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (query.isBlank()) stringResource(R.string.no_notes) else stringResource(
                        R.string.no_results
                    ),
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
                items(notes, key = { it.id }) { memo ->
                    SwipeableMemoItem(
                        memo = memo,
                        placeName = null,
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

@Composable
private fun TasksTab(viewModel: MemoListViewModel, onSetTime: (String) -> Unit) {
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val placesById by viewModel.placesById.collectAsStateWithLifecycle()
    val formatTimestamp = rememberDateFormatter()

    val planned = tasks
        .filter { it.remindAt != null || it.placeId != null }
        .sortedWith(compareBy(nullsLast<Long>()) { it.remindAt })
    val open = tasks.filter { it.remindAt == null && it.placeId == null }

    if (tasks.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.no_tasks),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (planned.isNotEmpty()) {
            item("h_planned") { SectionHeader(stringResource(R.string.tasks_planned)) }
            items(planned, key = { it.id }) { memo ->
                SwipeableMemoItem(
                    memo = memo,
                    placeName = memo.placeId?.toLongOrNull()?.let { placesById[it]?.name },
                    onDelete = { viewModel.delete(memo) },
                    onPin = { viewModel.togglePin(memo) },
                    onSetTime = onSetTime,
                    formatTimestamp = formatTimestamp
                )
            }
        }
        if (open.isNotEmpty()) {
            item("h_open") { SectionHeader(stringResource(R.string.tasks_open)) }
            items(open, key = { it.id }) { memo ->
                SwipeableMemoItem(
                    memo = memo,
                    placeName = null,
                    onDelete = { viewModel.delete(memo) },
                    onPin = { viewModel.togglePin(memo) },
                    onSetTime = onSetTime,
                    formatTimestamp = formatTimestamp
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
    )
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
        border = if (pinned) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            CategoryChip(category)
            Text(
                text = memo.text,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp)
            )
            if (category == Category.TAKEN) {
                when {
                    placeName != null -> InfoRow(
                        Icons.Filled.Place,
                        stringResource(R.string.at_place, placeName)
                    )

                    memo.recurrence != null && memo.remindAt != null ->
                        InfoRow(Icons.Filled.Repeat, formatTimestamp(memo.remindAt))

                    memo.remindAt != null ->
                        InfoRow(
                            Icons.Filled.Schedule,
                            stringResource(
                                R.string.reminder_prefix,
                                formatTimestamp(memo.remindAt)
                            ),
                            onClick = { onSetTime(memo.id) })

                    else ->
                        InfoRow(
                            Icons.Filled.Schedule, stringResource(R.string.add_time),
                            onClick = { onSetTime(memo.id) })
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
private fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.padding(end = 6.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
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
    Category.TAKEN -> Teal
    Category.IDEEEN -> AmberGold
    Category.AGENDA -> SteelAzure
    Category.OVERIG -> Platinum
}
