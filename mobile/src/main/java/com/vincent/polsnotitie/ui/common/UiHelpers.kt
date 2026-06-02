package com.vincent.polsnotitie.ui.common

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.vincent.polsnotitie.R
import com.vincent.polsnotitie.data.Category
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun Category.displayName(context: Context): String = context.getString(
    when (this) {
        Category.BOODSCHAPPEN  -> R.string.category_groceries
        Category.TODO          -> R.string.category_todo
        Category.IDEEEN        -> R.string.category_ideas
        Category.HERINNERINGEN -> R.string.category_reminders
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
