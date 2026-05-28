package com.vincent.polsnotitie.presentation

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.vincent.polsnotitie.presentation.theme.PolsnotitieTheme

class MainActivity : ComponentActivity() {
    private val autoStartTrigger = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent?.getBooleanExtra("autostart", false) == true) autoStartTrigger.intValue++
        setContent {
            MemoScreen(autoStartTrigger = autoStartTrigger.intValue)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra("autostart", false) == true) autoStartTrigger.intValue++
    }
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
                ?.firstOrNull()
                ?.trim()
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
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "nl-NL")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Spreek je notitie in")
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
        Status.Idle -> "Tik om in te spreken"
        Status.Sending -> "Verzenden…"
        Status.Sent -> "Verzonden ✓"
        Status.NothingHeard -> "Niets verstaan, opnieuw?"
        Status.Error -> "Versturen mislukt, opnieuw?"
        Status.NotAvailable -> "Spraakherkenning niet beschikbaar"
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
                Text("🎤  Inspreken")
            }
            Text(
                text = statusText,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}
