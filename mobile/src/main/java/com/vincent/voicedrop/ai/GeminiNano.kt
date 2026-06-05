package com.vincent.voicedrop.ai

import android.content.SharedPreferences
import android.util.Log
import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.common.GenAiException
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import com.google.mlkit.genai.prompt.TextPart
import com.google.mlkit.genai.prompt.generateContentRequest
import com.vincent.voicedrop.ai.GeminiNano.classify
import com.vincent.voicedrop.data.Category
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect

/**
 * On-device AI-terugval voor categorie-classificatie via Gemini Nano, aangesproken met de
 * **ML Kit GenAI Prompt API** (`com.google.mlkit:genai-prompt`). Dit is de officiële opvolger van
 * de oude experimentele `com.google.ai.edge.aicore`-SDK, die op recente AICore-builds
 * `NOT_AVAILABLE` gaf.
 *
 * Wordt alleen gebruikt als de snelle trefwoord-classificatie ([com.vincent.voicedrop.data.CategoryClassifier])
 * geen match vindt. Zo blijft het snelle pad gratis en deterministisch, en betalen we de AI-kosten
 * alleen voor zinnen die niet met een trefwoord beginnen.
 *
 * Degradeert netjes: als het model niet beschikbaar is (toestel zonder Nano, of nog niet
 * gedownload) geeft [classify] gewoon `null` terug en valt de app terug op het bestaande gedrag.
 *
 * Let op: de Prompt API is beta en de onderliggende preview-modellen zijn developer-preview,
 * niet voor productie.
 */
object GeminiNano {

    /** Status van het on-device model, voor weergave in Instellingen. */
    sealed interface Status {
        data object Unsupported :
            Status                                   // toestel ondersteunt Nano niet

        data object NotReady :
            Status                                      // beschikbaar om te downloaden

        data class Downloading(val downloadedBytes: Long, val totalBytes: Long) : Status
        data object Preparing : Status
        data object Ready :
            Status                                         // gedownload en bruikbaar

        data class Error(val message: String) : Status
    }

    private const val TAG = "GeminiNano"
    private const val PREF_ENABLED = "ai_fallback_enabled"

    private val _status = MutableStateFlow<Status>(Status.NotReady)
    val status: StateFlow<Status> = _status.asStateFlow()

    @Volatile
    private var model: GenerativeModel? = null
    @Volatile
    private var totalBytes: Long = 0L

    private fun model(): GenerativeModel = model ?: Generation.getClient().also { model = it }

    /** Of de gebruiker de AI-terugval heeft ingeschakeld (standaard aan). */
    fun isEnabled(prefs: SharedPreferences): Boolean = prefs.getBoolean(PREF_ENABLED, true)

    fun setEnabled(prefs: SharedPreferences, enabled: Boolean) =
        prefs.edit().putBoolean(PREF_ENABLED, enabled).apply()

    /** Leesbare detailtekst van een fout, inclusief ML Kit-errorcode indien beschikbaar. */
    private fun detail(t: Throwable): String {
        val code = (t as? GenAiException)?.errorCode
        val base = "${t.javaClass.simpleName}: ${t.message}"
        return if (code != null) "$base (errorCode=$code)" else base
    }

    private fun mapStatus(featureStatus: Int): Status = when (featureStatus) {
        FeatureStatus.AVAILABLE -> Status.Ready
        FeatureStatus.DOWNLOADABLE -> Status.NotReady
        FeatureStatus.DOWNLOADING -> Status.Downloading(0L, 0L)
        else -> Status.Unsupported
    }

    /** Passieve check voor Instellingen — start géén download. */
    suspend fun refreshStatus() {
        _status.value = try {
            val fs = model().checkStatus()
            Log.i(TAG, "checkStatus -> $fs")
            mapStatus(fs)
        } catch (t: Throwable) {
            Log.e(TAG, "checkStatus failed — ${detail(t)}", t)
            Status.Error(detail(t))
        }
    }

    /** Door de gebruiker gestart vanuit Instellingen: downloadt het model en volgt de voortgang. */
    suspend fun download() {
        Log.i(TAG, "download: start")
        _status.value = Status.Preparing
        try {
            model().download().collect { s ->
                when (s) {
                    is DownloadStatus.DownloadStarted -> {
                        Log.i(TAG, "downloadStarted: ${s.bytesToDownload} bytes")
                        totalBytes = s.bytesToDownload
                        _status.value = Status.Downloading(0L, s.bytesToDownload)
                    }

                    is DownloadStatus.DownloadProgress -> {
                        _status.value = Status.Downloading(s.totalBytesDownloaded, totalBytes)
                    }

                    DownloadStatus.DownloadCompleted -> {
                        Log.i(TAG, "downloadCompleted")
                        _status.value = Status.Ready
                    }

                    is DownloadStatus.DownloadFailed -> {
                        Log.e(TAG, "downloadFailed — ${detail(s.e)}", s.e)
                        _status.value = Status.Error(detail(s.e))
                    }
                }
            }
            // Flow afgelopen zonder fout? Bevestig de eindstatus via een verse check.
            if (_status.value !is Status.Error) refreshStatus()
        } catch (t: Throwable) {
            Log.e(TAG, "download failed — ${detail(t)}", t)
            _status.value = Status.Error(detail(t))
        }
    }

    /**
     * Classificeert een notitie via Nano. Geeft `null` als het model niet beschikbaar is of bij
     * twijfel (OVERIG), zodat de aanroeper terugvalt op het bestaande gedrag.
     */
    suspend fun classify(note: String): Category? {
        if (note.isBlank()) return null
        return try {
            val m = model()
            val fs = m.checkStatus()
            if (fs != FeatureStatus.AVAILABLE) {
                Log.d(TAG, "classify: skipped, featureStatus=$fs")
                return null
            }
            val request = generateContentRequest(TextPart(prompt(note))) {
                temperature = 0.2f          // laag = deterministischer, prima voor classificatie
                topK = 3
                maxOutputTokens = 24        // we hebben maar één woord nodig
            }
            val out = m.generateContent(request).candidates.firstOrNull()?.text
            val category = parse(out)
            Log.i(TAG, "classify: '$note' -> raw='$out' -> $category")
            category
        } catch (t: Throwable) {
            Log.w(TAG, "classify failed — ${detail(t)}", t)
            null
        }
    }

    private fun prompt(note: String): String {
        val safe = note.replace("\"", "'").take(400)
        return """
            You are a strict classifier for short personal notes. The note may be in any language.
            Choose exactly ONE category that fits best:
            BOODSCHAPPEN = groceries or shopping items to buy
            TAKEN = a task, to-do, or a reminder to do something (optionally at a time or place)
            AGENDA = an appointment or event for the calendar, tied to a date/time
            IDEEEN = an idea, thought, or something worth remembering
            OVERIG = none of the above
            Reply with ONLY the category word in UPPERCASE, nothing else.

            Note: "$safe"
            Category:
        """.trimIndent()
    }

    /** Pakt de eerste categorie-naam die in de modeluitvoer voorkomt; OVERIG/niets → null. */
    private fun parse(output: String?): Category? {
        val upper = output?.uppercase()?.trim() ?: return null
        return Category.entries.firstOrNull { it != Category.OVERIG && upper.contains(it.name) }
    }
}
