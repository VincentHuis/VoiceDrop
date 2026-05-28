package com.vincent.polsnotitie.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.vincent.polsnotitie.MainActivity
import com.vincent.polsnotitie.R

object ReminderNotifications {

    private const val CHANNEL_ID = "reminders"

    private fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Herinneringen",
                    NotificationManager.IMPORTANCE_HIGH
                )
            )
        }
    }

    private fun addTimeId(memoId: String) = ("add$memoId").hashCode()
    private fun fireId(memoId: String) = ("fire$memoId").hashCode()

    /** Geen tijd herkend: vraag de gebruiker om er een toe te voegen. */
    fun notifyAddTime(context: Context, memoId: String, text: String) {
        ensureChannel(context)
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("setTimeForMemo", memoId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context, addTimeId(memoId), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_reminder)
            .setContentTitle("Herinnering — tijd toevoegen")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()
        notify(context, addTimeId(memoId), notification)
    }

    /** Het herinneringsmoment is bereikt. */
    fun notifyReminder(context: Context, memoId: String, text: String) {
        ensureChannel(context)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context, fireId(memoId), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_reminder)
            .setContentTitle("Herinnering")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()
        notify(context, fireId(memoId), notification)
    }

    fun cancelAddTime(context: Context, memoId: String) {
        NotificationManagerCompat.from(context).cancel(addTimeId(memoId))
    }

    /** Bij aankomst supermarkt: 1-3 boodschappen in de melding, anders verwijzen naar de app. */
    fun notifyShopping(context: Context, items: List<String>) {
        ensureChannel(context)
        val text = if (items.size <= 3) {
            items.joinToString(", ")
        } else {
            "Je hebt ${items.size} boodschappen — open de app"
        }
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("openShopping", true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context, "shopping".hashCode(), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_reminder)
            .setContentTitle("Boodschappen")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()
        notify(context, "shopping".hashCode(), notification)
    }

    private fun notify(context: Context, id: Int, notification: android.app.Notification) {
        val manager = NotificationManagerCompat.from(context)
        if (manager.areNotificationsEnabled()) {
            try {
                manager.notify(id, notification)
            } catch (e: SecurityException) {
                // POST_NOTIFICATIONS niet verleend; stilletjes overslaan.
            }
        }
    }
}
