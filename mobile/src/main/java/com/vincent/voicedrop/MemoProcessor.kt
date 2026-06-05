package com.vincent.voicedrop

import android.content.Context
import com.vincent.voicedrop.calendar.CalendarHelper
import com.vincent.voicedrop.data.Category
import com.vincent.voicedrop.data.CategoryClassifier
import com.vincent.voicedrop.data.Memo
import com.vincent.voicedrop.data.MemoDatabase
import com.vincent.voicedrop.language.LanguageProvider
import com.vincent.voicedrop.location.GeofenceManager
import com.vincent.voicedrop.reminder.PlaceParser
import com.vincent.voicedrop.reminder.ReminderNotifications
import com.vincent.voicedrop.reminder.ReminderScheduler
import com.vincent.voicedrop.reminder.ReminderTimeParser
import com.vincent.voicedrop.widget.ShoppingWidget

class MemoProcessor(private val context: Context) {

    private val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    private val config = LanguageProvider.get(prefs)
    private val classifier = CategoryClassifier()
    private val timeParser = ReminderTimeParser(config)

    suspend fun process(id: String, rawText: String, timestamp: Long) {
        val dao = MemoDatabase.get(context).memoDao()
        val placeDao = MemoDatabase.get(context).placeDao()
        val classified = classifier.classify(rawText)

        if (classified.category == Category.AGENDA) {
            handleAgenda(classified.text)
            return
        }

        var text = classified.text
        var remindAt: Long? = null
        var placeId: String? = null
        if (classified.category == Category.HERINNERINGEN) {
            val places = placeDao.getAllNow()
            val placeResult = PlaceParser(places).parse(text)
            if (placeResult.place != null) {
                placeId = placeResult.place.id.toString()
                text = placeResult.text
            } else {
                val parsed = timeParser.parse(text)
                text = parsed.text.ifEmpty { text }
                remindAt = parsed.remindAt
            }
        }

        val memo = Memo(
            id = id, text = text, timestamp = timestamp,
            category = classified.category.name, remindAt = remindAt, placeId = placeId
        )
        dao.insert(memo)

        if (classified.category == Category.HERINNERINGEN) {
            when {
                placeId != null  -> GeofenceManager.registerAll(context)
                remindAt != null -> ReminderScheduler.schedule(context, memo)
                else             -> ReminderNotifications.notifyAddTime(context, memo.id, memo.text)
            }
        }
        ShoppingWidget.refresh(context)
    }

    private fun handleAgenda(rawText: String) {
        val parsed = timeParser.parse(rawText)
        val title = parsed.text.ifEmpty { rawText }
        val start = parsed.remindAt
        if (start != null && CalendarHelper.insertEvent(context, title, start)) {
            CalendarHelper.notifyPlanned(context, title, start)
        } else {
            CalendarHelper.notifyAddToCalendar(context, title, start)
        }
    }
}
