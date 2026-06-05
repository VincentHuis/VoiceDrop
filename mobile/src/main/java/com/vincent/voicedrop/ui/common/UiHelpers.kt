package com.vincent.voicedrop.ui.common

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.vincent.voicedrop.R
import com.vincent.voicedrop.data.Category
import com.vincent.voicedrop.data.PlaceType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun PlaceType.displayName(context: Context): String = context.getString(
    when (this) {
        PlaceType.THUIS      -> R.string.place_home
        PlaceType.WERK       -> R.string.place_work
        PlaceType.SUPERMARKT -> R.string.place_supermarket
        PlaceType.VRIENDEN   -> R.string.place_friends
        PlaceType.OVERIGE    -> R.string.place_other_type
    }
)

fun Category.displayName(context: Context): String = context.getString(
    when (this) {
        Category.BOODSCHAPPEN  -> R.string.category_groceries
        Category.TAKEN -> R.string.category_taken
        Category.IDEEEN        -> R.string.category_ideas
        Category.AGENDA        -> R.string.category_agenda
        Category.OVERIG        -> R.string.category_other
    }
)

fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("memo", text))
}

fun shareText(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, null))
}

@Composable
fun rememberDateFormatter(): (Long) -> String {
    val context = LocalContext.current
    return remember(context) {
        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val langCode = prefs.getString("app_language", "nl") ?: "nl"
        val formatter = SimpleDateFormat("d MMM yyyy, HH:mm", Locale.forLanguageTag(langCode))
        ({ timestamp: Long -> formatter.format(Date(timestamp)) })
    }
}
