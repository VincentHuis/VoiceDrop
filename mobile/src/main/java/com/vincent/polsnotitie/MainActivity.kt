package com.vincent.polsnotitie

import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.vincent.polsnotitie.language.withAppLanguage
import com.vincent.polsnotitie.location.GeofenceManager
import com.vincent.polsnotitie.ui.screens.MapPickerScreen
import com.vincent.polsnotitie.ui.screens.MemoListScreen
import com.vincent.polsnotitie.ui.screens.PlacesScreen
import com.vincent.polsnotitie.ui.screens.ReminderTimeScreen
import com.vincent.polsnotitie.ui.screens.SettingsScreen
import com.vincent.polsnotitie.ui.screens.ShoppingScreen
import com.vincent.polsnotitie.ui.theme.PolsnotitieTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        val prefs = newBase.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        super.attachBaseContext(newBase.withAppLanguage(prefs))
    }

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
    // -1L = nieuwe plek; 0L/positief = bestaande id; null = picker dicht.
    var mapPickerPlaceId by rememberSaveable { mutableStateOf<Long?>(null) }

    when {
        mapPickerPlaceId != null -> MapPickerScreen(
            placeId = mapPickerPlaceId!!.takeIf { it >= 0 },
            onBack = { mapPickerPlaceId = null }
        )
        showPlaces -> PlacesScreen(
            onBack = { showPlaces = false },
            onEdit = { id -> mapPickerPlaceId = id ?: -1L }
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
