package com.vincent.polsnotitie.location

import android.content.Context
import android.content.BroadcastReceiver
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.vincent.polsnotitie.data.Category
import com.vincent.polsnotitie.data.MemoDatabase
import com.vincent.polsnotitie.data.PlaceType
import com.vincent.polsnotitie.reminder.ReminderNotifications

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) return
        if (event.geofenceTransition != Geofence.GEOFENCE_TRANSITION_ENTER) return
        val placeIds = event.triggeringGeofences?.map { it.requestId } ?: return

        val pending = goAsync()
        Thread {
            try {
                val db = MemoDatabase.get(context)
                val dao = db.memoDao()
                val placeDao = db.placeDao()
                for (idStr in placeIds) {
                    val id = idStr.toLongOrNull() ?: continue
                    val place = placeDao.getAllNow().firstOrNull { it.id == id } ?: continue
                    if (place.type == PlaceType.SUPERMARKT) {
                        handleSupermarkt(context, dao)
                    }
                    handlePlaceReminders(context, dao, idStr)
                }
            } finally {
                pending.finish()
            }
        }.start()
    }

    private fun handlePlaceReminders(
        context: Context,
        dao: com.vincent.polsnotitie.data.MemoDao,
        placeId: String
    ) {
        val reminders = dao.byPlaceNow(placeId)
        for (memo in reminders) {
            ReminderNotifications.notifyReminder(context, memo.id, memo.text)
            dao.deleteByIdNow(memo.id) // locatie-herinnering vuurt eenmalig
        }
    }

    private fun handleSupermarkt(context: Context, dao: com.vincent.polsnotitie.data.MemoDao) {
        val items = dao.byCategoryUncheckedNow(Category.BOODSCHAPPEN.name).map { it.text }
        if (items.isEmpty()) return
        ReminderNotifications.notifyShopping(context, items)
    }
}
