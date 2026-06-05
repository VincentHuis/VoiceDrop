package com.vincent.voicedrop.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.vincent.voicedrop.R
import com.vincent.voicedrop.ai.GeminiNano
import com.vincent.voicedrop.language.AppLanguage
import com.vincent.voicedrop.language.LanguagePreference
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, onOpenPlaces: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE) }
    var selectedLang by remember { mutableStateOf(LanguagePreference.get(prefs)) }
    val dataClient = remember { Wearable.getDataClient(context) }

    val scope = rememberCoroutineScope()
    val aiStatus by GeminiNano.status.collectAsState()
    var aiEnabled by remember { mutableStateOf(GeminiNano.isEnabled(prefs)) }
    LaunchedEffect(Unit) { GeminiNano.refreshStatus() }

    BackHandler(onBack = onBack)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back_label))
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
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        stringResource(R.string.language_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    AppLanguage.entries.forEach { lang ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedLang = lang
                                    LanguagePreference.set(prefs, lang)
                                    syncLanguageToWatch(dataClient, lang)
                                    (context as? android.app.Activity)?.recreate()
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedLang == lang,
                                onClick = {
                                    selectedLang = lang
                                    LanguagePreference.set(prefs, lang)
                                    syncLanguageToWatch(dataClient, lang)
                                    (context as? android.app.Activity)?.recreate()
                                }
                            )
                            Text(lang.displayName, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }

            Card(modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenPlaces() }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Place, contentDescription = null,
                        modifier = Modifier.padding(end = 12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.locations_title),
                            style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.locations_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        stringResource(R.string.ai_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.ai_explanation),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    when (val s = aiStatus) {
                        GeminiNano.Status.Unsupported -> Text(
                            stringResource(R.string.ai_unsupported),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        GeminiNano.Status.NotReady -> Column {
                            Text(stringResource(R.string.ai_downloadable))
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = {
                                scope.launch { GeminiNano.download() }
                            }) { Text(stringResource(R.string.ai_download_button)) }
                        }

                        is GeminiNano.Status.Downloading -> Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                            val suffix = if (s.totalBytes > 0)
                                " (${s.downloadedBytes / 1_000_000} / ${s.totalBytes / 1_000_000} MB)" else ""
                            Text(
                                stringResource(R.string.ai_downloading) + suffix,
                                modifier = Modifier.padding(start = 12.dp)
                            )
                        }

                        GeminiNano.Status.Preparing -> Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                            Text(
                                stringResource(R.string.ai_checking),
                                modifier = Modifier.padding(start = 12.dp)
                            )
                        }

                        GeminiNano.Status.Ready -> Column {
                            Text(
                                stringResource(R.string.ai_ready),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Switch(checked = aiEnabled, onCheckedChange = {
                                    aiEnabled = it
                                    GeminiNano.setEnabled(prefs, it)
                                })
                                Text(
                                    stringResource(R.string.ai_use_fallback),
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }

                        is GeminiNano.Status.Error -> Text(
                            stringResource(R.string.ai_error, s.message),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

private fun syncLanguageToWatch(dataClient: DataClient, lang: AppLanguage) {
    val request = PutDataMapRequest.create("/settings/language").apply {
        dataMap.putString("language", lang.code)
    }.asPutDataRequest().setUrgent()
    dataClient.putDataItem(request)
}
