package com.vincent.voicedrop

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.vincent.voicedrop.language.withAppLanguage
import com.vincent.voicedrop.location.GeofenceManager
import com.vincent.voicedrop.ui.screens.MapPickerScreen
import com.vincent.voicedrop.ui.screens.MemoListScreen
import com.vincent.voicedrop.ui.screens.OnboardingScreen
import com.vincent.voicedrop.ui.screens.PlacesScreen
import com.vincent.voicedrop.ui.screens.ReminderTimeScreen
import com.vincent.voicedrop.ui.screens.SettingsScreen
import com.vincent.voicedrop.ui.screens.ShoppingScreen
import com.vincent.voicedrop.ui.theme.VoicedropTheme
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
            VoicedropTheme {
                AppRoot(startInShopping = startInShopping, startTimeForMemoId = timeForMemo)
            }
        }
    }
}

@Composable
fun AppRoot(startInShopping: Boolean = false, startTimeForMemoId: String? = null) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("app_settings", Context.MODE_PRIVATE) }
    var onboardingDone by rememberSaveable { mutableStateOf(prefs.getBoolean("onboarding_completed", false)) }

    if (!onboardingDone) {
        OnboardingScreen(onDone = {
            prefs.edit().putBoolean("onboarding_completed", true).apply()
            onboardingDone = true
        })
        return
    }

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

