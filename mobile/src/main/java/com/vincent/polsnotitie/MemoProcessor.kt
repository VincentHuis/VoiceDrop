package com.vincent.polsnotitie

import android.content.Context
import com.vincent.polsnotitie.calendar.CalendarHelper
import com.vincent.polsnotitie.data.Category
import com.vincent.polsnotitie.data.CategoryClassifier
import com.vincent.polsnotitie.data.Memo
import com.vincent.polsnotitie.language.configs.NlLanguageConfig
import com.vincent.polsnotitie.data.MemoDatabase
import com.vincent.polsnotitie.location.GeofenceManager
import com.vincent.polsnotitie.reminder.PlaceParser
import com.vincent.polsnotitie.reminder.ReminderNotifications
import com.vincent.polsnotitie.reminder.ReminderScheduler
import com.vincent.polsnotitie.reminder.ReminderTimeParser
import com.vincent.polsnotitie.widget.ShoppingWidget

/**
 * Verwerkt een ingesproken tekst tot het juiste gevolg: classificeren, agenda inplannen,
 * herinnering (tijd of plek) of gewone memo opslaan. Gedeeld door de Data Layer-listener
 * (vanaf het horloge) en de microfoonknop in de telefoon-app.
 */
object MemoProcessor {

    suspend fun process(context: Context, id: String, rawText: String, timestamp: Long) {
        val dao = MemoDatabase.get(context).memoDao()
        val classified = CategoryClassifier(NlLanguageConfig).classify(rawText)

        if (classified.category == Category.AGENDA) {
            handleAgenda(context, classified.text)
            return
        }

        var text = classified.text
        var remindAt: Long? = null
        var placeId: String? = null
        if (classified.category == Category.HERINNERINGEN) {
            val placeResult = PlaceParser.parse(text)
            if (placeResult.place != null) {
                placeId = placeResult.place.name
                text = placeResult.text
            } else {
                val parsed = ReminderTimeParser.parse(text)
                text = parsed.text.ifEmpty { text }
                remindAt = parsed.remindAt
            }
        }

        val memo = Memo(
            id = id,
            text = text,
            timestamp = timestamp,
            category = classified.category.name,
            remindAt = remindAt,
            placeId = placeId
        )
        dao.insert(memo)

        if (classified.category == Category.HERINNERINGEN) {
            when {
                placeId != null -> GeofenceManager.registerAll(context)
                remindAt != null -> ReminderScheduler.schedule(context, memo)
                else -> ReminderNotifications.notifyAddTime(context, memo.id, memo.text)
            }
        }
        ShoppingWidget.refresh(context)
    }

    private fun handleAgenda(context: Context, rawText: String) {
        val parsed = ReminderTimeParser.parse(rawText)
        val title = parsed.text.ifEmpty { rawText }
        val start = parsed.remindAt
        if (start != null && CalendarHelper.insertEvent(context, title, start)) {
            CalendarHelper.notifyPlanned(context, title, start)
        } else {
            CalendarHelper.notifyAddToCalendar(context, title, start)
        }
    }
}
