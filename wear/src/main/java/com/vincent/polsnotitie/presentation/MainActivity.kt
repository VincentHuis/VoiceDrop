package com.vincent.polsnotitie.presentation

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import com.vincent.polsnotitie.R
import com.vincent.polsnotitie.presentation.theme.PolsnotitieTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

private const val PREFS_NAME = "wear_settings"
private const val KEY_LANGUAGE = "wear_language"

class MainActivity : ComponentActivity() {
    private val autoStartTrigger = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent?.getBooleanExtra("autostart", false) == true) autoStartTrigger.intValue++
        val localizedCtx = applyLanguage(this)
        setContent {
            CompositionLocalProvider(LocalContext provides localizedCtx) {
                MemoScreen(autoStartTrigger = autoStartTrigger.intValue)
            }
        }
        refreshLanguageFromDataLayer()
    }

    private fun refreshLanguageFromDataLayer() {
        lifecycleScope.launch(Dispatchers.IO) {
            val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val current = prefs.getString(KEY_LANGUAGE, "nl") ?: "nl"
            val remote = try {
                val uri = Uri.parse("wear://*/settings/language")
                val items = Tasks.await(Wearable.getDataClient(applicationContext).getDataItems(uri))
                val code = items.firstOrNull()
                    ?.let { DataMapItem.fromDataItem(it).dataMap.getString("language") }
                items.release()
                code
            } catch (e: Exception) {
                null
            }
            if (remote != null && remote != current) {
                prefs.edit().putString(KEY_LANGUAGE, remote).apply()
                withContext(Dispatchers.Main) { recreate() }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra("autostart", false) == true) autoStartTrigger.intValue++
    }
}

private fun applyLanguage(context: Context): Context {
    val code = resolveLanguageCode(context)
    val locale = Locale.forLanguageTag(code)
    val config = Configuration(context.resources.configuration).also { it.setLocale(locale) }
    return context.createConfigurationContext(config)
}

private fun resolveLanguageCode(context: Context): String {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getString(KEY_LANGUAGE, "nl") ?: "nl"
}

private enum class Status { Idle, Sending, Sent, NothingHeard, Error, NotAvailable }

@Composable
fun MemoScreen(autoStartTrigger: Int = 0) {
    val context = LocalContext.current
    var status by remember { mutableStateOf(Status.Idle) }

    val launcher = rememberLauncherForActivityResult(StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val text = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()?.trim()
            if (!text.isNullOrEmpty()) {
                status = Status.Sending
                MemoSender.send(context, text) { ok ->
                    status = if (ok) Status.Sent else Status.Error
                }
            } else {
                status = Status.NothingHeard
            }
        } else {
            status = Status.Idle
        }
    }

    fun startRecognition() {
        val code = resolveLanguageCode(context)
        val locale = when (code) {
            "de" -> "de-DE"
            "en" -> "en-GB"
            else -> "nl-NL"
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale)
            putExtra(RecognizerIntent.EXTRA_PROMPT, context.getString(R.string.speech_prompt))
        }
        try {
            launcher.launch(intent)
        } catch (e: ActivityNotFoundException) {
            status = Status.NotAvailable
        }
    }

    LaunchedEffect(autoStartTrigger) {
        if (autoStartTrigger > 0) startRecognition()
    }

    val statusText = when (status) {
        Status.Idle         -> stringResource(R.string.tap_to_record)
        Status.Sending      -> stringResource(R.string.sending)
        Status.Sent         -> stringResource(R.string.sent)
        Status.NothingHeard -> stringResource(R.string.nothing_heard)
        Status.Error        -> stringResource(R.string.send_failed)
        Status.NotAvailable -> stringResource(R.string.not_available)
    }

    PolsnotitieTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
        ) {
            Button(onClick = { startRecognition() }) {
                Text("🎤  ${stringResource(R.string.record_button)}")
            }
            Text(
                text = statusText,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}
