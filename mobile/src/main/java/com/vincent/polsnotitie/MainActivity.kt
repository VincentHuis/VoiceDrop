package com.vincent.polsnotitie

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.delay
import com.vincent.polsnotitie.data.Category
import com.vincent.polsnotitie.data.Memo
import com.vincent.polsnotitie.data.MemoDao
import com.vincent.polsnotitie.data.MemoDatabase
import com.vincent.polsnotitie.data.Place
import com.vincent.polsnotitie.data.PlaceType
import com.vincent.polsnotitie.location.GeocoderHelper
import com.vincent.polsnotitie.location.GeofenceManager
import com.vincent.polsnotitie.reminder.ReminderNotifications
import com.vincent.polsnotitie.reminder.ReminderScheduler
import com.vincent.polsnotitie.ui.theme.AmberGold
import com.vincent.polsnotitie.ui.theme.BalticBlue
import com.vincent.polsnotitie.ui.theme.InkDark
import com.vincent.polsnotitie.ui.theme.Platinum
import com.vincent.polsnotitie.ui.theme.PolsnotitieTheme
import com.vincent.polsnotitie.ui.theme.SteelAzure
import com.vincent.polsnotitie.ui.theme.VibrantCoral
import com.vincent.polsnotitie.widget.ShoppingWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val startInShopping = intent?.getBooleanExtra("openShopping", false) == true
        val timeForMemo = intent?.getStringExtra("setTimeForMemo")
        setContent {
            PolsnotitieTheme {
                AppRoot(startInShopping = startInShopping, startTimeForMemoId = timeForMemo)
            }
        }
    }
}

@Composable
fun AppRoot(startInShopping: Boolean = false, startTimeForMemoId: String? = null) {
    RequestPermissions()
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) { GeofenceManager.registerAll(context) }
    }

    var showShopping by rememberSaveable { mutableStateOf(startInShopping) }
    var timeMemoId by rememberSaveable { mutableStateOf(startTimeForMemoId) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showPlaces by rememberSaveable { mutableStateOf(false) }
    var mapPickerType by rememberSaveable { mutableStateOf<String?>(null) }

    when {
        mapPickerType != null -> MapPickerScreen(
            type = PlaceType.valueOf(mapPickerType!!),
            onBack = { mapPickerType = null }
        )
        showPlaces -> PlacesScreen(
            onBack = { showPlaces = false },
            onPick = { type -> mapPickerType = type.name }
        )
        showSettings -> SettingsScreen(
            onBack = { showSettings = false },
            onOpenPlaces = { showPlaces = true }
        )
        timeMemoId != null -> ReminderTimeScreen(
            memoId = timeMemoId!!,
            onDone = { timeMemoId = null }
        )
        showShopping -> ShoppingScreen(onBack = { showShopping = false })
        else -> MemoListScreen(
            onOpenShopping = { showShopping = true },
            onOpenSettings = { showSettings = true },
            onSetTime = { id -> timeMemoId = id }
        )
    }
}

@Composable
private fun RequestPermissions() {
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {}
    LaunchedEffect(Unit) {
        val perms = buildList {
            add(android.Manifest.permission.READ_CALENDAR)
            add(android.Manifest.permission.WRITE_CALENDAR)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        launcher.launch(perms.toTypedArray())
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoListScreen(
    onOpenShopping: () -> Unit,
    onOpenSettings: () -> Unit,
    onSetTime: (String) -> Unit
) {
    val context = LocalContext.current
    val dao = remember { MemoDatabase.get(context).memoDao() }
    val scope = rememberCoroutineScope()

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
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "nl-NL")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Spreek je notitie in")
        }
        try {
            micLauncher.launch(intent)
        } catch (e: ActivityNotFoundException) {
            // Geen spraakherkenning beschikbaar.
        }
    }

    var query by remember { mutableStateOf("") }
    val memos by remember(query) {
        val excluded = Category.BOODSCHAPPEN.name
        if (query.isBlank()) dao.getAll(excluded) else dao.search(query, excluded)
    }.collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("VoiceDrop") },
                actions = {
                    TextButton(onClick = onOpenShopping) {
                        Icon(
                            Icons.Filled.ShoppingCart,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Text("Boodschappen")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Instellingen")
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
                    onValueChange = { query = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    placeholder = { Text("Zoeken…") }
                )
                IconButton(onClick = { startMic() }) {
                    Icon(Icons.Filled.Mic, contentDescription = "Inspreken")
                }
            }

            if (memos.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (query.isBlank()) "Nog geen notities" else "Geen resultaten",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 16.dp,
                        vertical = 8.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(memos, key = { it.id }) { memo ->
                        SwipeableMemoItem(
                            memo = memo,
                            onDelete = { scope.launch { dao.delete(memo) } },
                            onPin = {
                                scope.launch {
                                    dao.setPinned(memo.id, if (memo.pinnedAt == null) System.currentTimeMillis() else null)
                                }
                            },
                            onSetTime = onSetTime
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableMemoItem(memo: Memo, onDelete: () -> Unit, onPin: () -> Unit, onSetTime: (String) -> Unit) {
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
                    contentDescription = "Verwijderen",
                    tint = Color.White
                )
            }
        }
    ) {
        MemoCard(memo = memo, onPin = onPin, onSetTime = onSetTime)
    }
}

@Composable
private fun MemoCard(memo: Memo, onPin: () -> Unit, onSetTime: (String) -> Unit) {
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
                val place = PlaceType.fromName(memo.placeId)
                if (place != null) {
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
                            text = "Bij ${place.displayName}",
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
                            text = memo.remindAt?.let { "Herinnering: ${formatTimestamp(it)}" }
                                ?: "Tijd toevoegen",
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
                        contentDescription = if (pinned) "Losmaken" else "Vastpinnen",
                        tint = if (pinned) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { copyToClipboard(context, memo.text) }) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = "Kopiëren")
                }
                IconButton(onClick = { shareText(context, memo.text) }) {
                    Icon(Icons.Filled.Share, contentDescription = "Delen")
                }
            }
        }
    }
}

private const val CHECKED_TTL_MS = 15 * 60 * 1000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val dao = remember { MemoDatabase.get(context).memoDao() }
    val scope = rememberCoroutineScope()

    val items by remember { dao.byCategory(Category.BOODSCHAPPEN.name) }
        .collectAsState(initial = emptyList())

    // Lui opruimen: afgevinkte producten verdwijnen 15 min na het aanvinken.
    LaunchedEffect(Unit) {
        while (true) {
            dao.deleteCheckedBefore(System.currentTimeMillis() - CHECKED_TTL_MS)
            ShoppingWidget.refresh(context)
            delay(30_000)
        }
    }

    BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Boodschappen") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Terug")
                    }
                }
            )
        }
    ) { padding ->
        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Geen boodschappen",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                items(items, key = { it.id }) { memo ->
                    val checked = memo.checkedAt != null
                    fun toggle(value: Boolean) {
                        scope.launch {
                            dao.setChecked(memo.id, if (value) System.currentTimeMillis() else null)
                            ShoppingWidget.refresh(context)
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { toggle(!checked) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = checked, onCheckedChange = { toggle(it) })
                        Text(
                            text = memo.text,
                            style = MaterialTheme.typography.bodyLarge,
                            textDecoration = if (checked) TextDecoration.LineThrough else null,
                            color = if (checked) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
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
            text = category.displayName,
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
    Category.AGENDA -> SteelAzure   // wordt niet opgeslagen → chip nooit getoond
    Category.OVERIG -> Platinum
}

private fun formatTimestamp(timestamp: Long): String =
    SimpleDateFormat("d MMM yyyy, HH:mm", Locale.forLanguageTag("nl")).format(Date(timestamp))

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("memo", text))
}

private fun shareText(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, null))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderTimeScreen(memoId: String, onDone: () -> Unit) {
    val context = LocalContext.current
    val dao = remember { MemoDatabase.get(context).memoDao() }
    val scope = rememberCoroutineScope()
    var memo by remember { mutableStateOf<Memo?>(null) }

    LaunchedEffect(memoId) { memo = dao.getById(memoId) }
    BackHandler(onBack = onDone)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tijd instellen") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Terug")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = memo?.text ?: "",
                style = MaterialTheme.typography.headlineSmall
            )
            memo?.remindAt?.let {
                Text(
                    text = "Huidige tijd: ${formatTimestamp(it)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(
                onClick = {
                    pickDateTime(context) { millis ->
                        scope.launch {
                            dao.setRemindAt(memoId, millis)
                            dao.getById(memoId)?.let { ReminderScheduler.schedule(context, it) }
                            ReminderNotifications.cancelAddTime(context, memoId)
                            onDone()
                        }
                    }
                }
            ) {
                Icon(
                    Icons.Filled.Schedule,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text("Kies datum en tijd")
            }
        }
    }
}

private fun pickDateTime(context: Context, onPicked: (Long) -> Unit) {
    val now = Calendar.getInstance()
    DatePickerDialog(
        context,
        { _, year, month, day ->
            TimePickerDialog(
                context,
                { _, hour, minute ->
                    val c = Calendar.getInstance().apply {
                        set(year, month, day, hour, minute, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    onPicked(c.timeInMillis)
                },
                now.get(Calendar.HOUR_OF_DAY),
                now.get(Calendar.MINUTE),
                true
            ).show()
        },
        now.get(Calendar.YEAR),
        now.get(Calendar.MONTH),
        now.get(Calendar.DAY_OF_MONTH)
    ).show()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, onOpenPlaces: () -> Unit) {
    BackHandler(onBack = onBack)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Instellingen") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Terug")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenPlaces() }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Place,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Locaties", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Thuis, werk en supermarkt instellen",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlacesScreen(onBack: () -> Unit, onPick: (PlaceType) -> Unit) {
    val context = LocalContext.current
    val dao = remember { MemoDatabase.get(context).placeDao() }
    val places by remember { dao.getAll() }.collectAsState(initial = emptyList())
    val placeByType = places.associateBy { it.type }

    val bgLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}

    BackHandler(onBack = onBack)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Plekken") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Terug")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Tik een plek aan en zoek een adres of zet hem op de kaart (100 m eromheen). " +
                    "Voor meldingen als de app dicht is, zet locatie op 'Altijd toestaan'.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Button(onClick = {
                    bgLauncher.launch(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                }) {
                    Text("Locatie altijd toestaan")
                }
            }
            for (type in PlaceType.entries) {
                Card(modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPick(type) }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Place,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        val saved = placeByType[type.name]
                        Column(modifier = Modifier.weight(1f)) {
                            Text(type.displayName, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = saved?.address
                                    ?: if (saved != null) "Ingesteld" else "Niet ingesteld",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapPickerScreen(type: PlaceType, onBack: () -> Unit) {
    val context = LocalContext.current
    val dao = remember { MemoDatabase.get(context).placeDao() }
    val scope = rememberCoroutineScope()
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var query by remember { mutableStateOf("") }
    var searchError by remember { mutableStateOf(false) }
    val fused = remember { LocationServices.getFusedLocationProviderClient(context) }

    fun searchAddress() {
        if (query.isBlank()) return
        scope.launch {
            val point = GeocoderHelper.search(context, query)
            if (point != null) {
                searchError = false
                mapView?.controller?.animateTo(point)
                mapView?.controller?.setZoom(17.0)
            } else {
                searchError = true
            }
        }
    }

    fun hasLocation() = ContextCompat.checkSelfPermission(
        context, android.Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    // Snel en zonder glitch: gecachte laatste locatie, direct centreren (geen animatie).
    fun centerOnLastLocation() {
        if (!hasLocation()) return
        try {
            fused.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    mapView?.controller?.setCenter(GeoPoint(loc.latitude, loc.longitude))
                }
            }
        } catch (e: SecurityException) {
        }
    }

    // Verse fix bij tikken op de knop.
    fun goToCurrentLocation() {
        if (!hasLocation()) return
        try {
            fused.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                .addOnSuccessListener { loc ->
                    if (loc != null) {
                        mapView?.controller?.animateTo(GeoPoint(loc.latitude, loc.longitude))
                        mapView?.controller?.setZoom(16.0)
                    }
                }
        } catch (e: SecurityException) {
        }
    }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result[android.Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            centerOnLastLocation()
        }
    }
    LaunchedEffect(Unit) {
        permLauncher.launch(
            arrayOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    BackHandler(onBack = onBack)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kies ${type.displayName}") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Terug")
                    }
                },
                actions = {
                    IconButton(onClick = { goToCurrentLocation() }) {
                        Icon(Icons.Filled.MyLocation, contentDescription = "Huidige locatie")
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
            OutlinedTextField(
                value = query,
                onValueChange = { query = it; searchError = false },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true,
                placeholder = { Text("Example: Herestraat, Groningen, The Netherlands") },
                isError = searchError,
                supportingText = if (searchError) {
                    { Text("Adres niet gevonden") }
                } else null,
                trailingIcon = {
                    IconButton(onClick = { searchAddress() }) {
                        Icon(Icons.Filled.Search, contentDescription = "Zoek adres")
                    }
                }
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp)
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        Configuration.getInstance().userAgentValue = ctx.packageName
                        val map = MapView(ctx)
                        map.setTileSource(TileSourceFactory.MAPNIK)
                        map.setMultiTouchControls(true)
                        map.controller.setZoom(15.0)
                        map.controller.setCenter(GeoPoint(52.3676, 4.9041))
                        map.onResume()
                        mapView = map
                        map
                    }
                )
                // Vaste rode pin in het midden; de tip wijst naar het middelpunt.
                Icon(
                    Icons.Filled.Place,
                    contentDescription = null,
                    tint = VibrantCoral,
                    modifier = Modifier
                        .size(48.dp)
                        .offset(y = (-24).dp)
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = {
                    val center = mapView?.mapCenter ?: return@Button
                    scope.launch {
                        val address = GeocoderHelper.addressOf(
                            context, center.latitude, center.longitude
                        )
                        dao.upsert(
                            Place(
                                type = type.name,
                                lat = center.latitude,
                                lng = center.longitude,
                                address = address
                            )
                        )
                        withContext(Dispatchers.IO) { GeofenceManager.registerAll(context) }
                        onBack()
                    }
                },
                enabled = mapView != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("Opslaan")
            }
        }
    }
}
