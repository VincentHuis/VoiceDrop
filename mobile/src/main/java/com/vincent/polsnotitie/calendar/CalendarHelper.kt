package com.vincent.polsnotitie.calendar

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.vincent.polsnotitie.R
import java.util.TimeZone

/**
 * Zet een "agenda"-memo om in een afspraak. Bij voldoende rechten + een herkende tijd wordt de
 * afspraak direct in de (Google-)agenda gezet via de Calendar-provider; anders volgt een
 * notificatie die de agenda-app opent met de afspraak voorgevuld.
 */
object CalendarHelper {

    private const val CHANNEL_ID = "agenda"
    private const val DEFAULT_DURATION_MS = 60 * 60 * 1000L

    fun canWrite(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED

    /** Probeert de afspraak direct in te plannen. Geeft true bij succes. */
    fun insertEvent(context: Context, title: String, startMillis: Long): Boolean {
        if (!canWrite(context)) return false
        val calendarId = findCalendarId(context) ?: return false
        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.TITLE, title)
            put(CalendarContract.Events.DTSTART, startMillis)
            put(CalendarContract.Events.DTEND, startMillis + DEFAULT_DURATION_MS)
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
        }
        return try {
            context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values) != null
        } catch (e: SecurityException) {
            false
        }
    }

    /** Kiest bij voorkeur de primaire Google-agenda waar je eigenaar van bent. */
    private fun findCalendarId(context: Context): Long? {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.ACCOUNT_TYPE,
            CalendarContract.Calendars.IS_PRIMARY
        )
        val selection = "${CalendarContract.Calendars.VISIBLE} = 1 AND " +
            "${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} >= " +
            "${CalendarContract.Calendars.CAL_ACCESS_OWNER}"
        return try {
            context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI, projection, selection, null, null
            )?.use { cursor ->
                var fallback: Long? = null
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(0)
                    val accountType = cursor.getString(1)
                    val isPrimary = cursor.getInt(2) == 1
                    if (accountType == "com.google" && isPrimary) return id
                    if (fallback == null) fallback = id
                }
                fallback
            }
        } catch (e: SecurityException) {
            null
        }
    }

    fun notifyPlanned(context: Context, title: String, startMillis: Long) {
        ensureChannel(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_reminder)
            .setContentTitle("Afspraak ingepland")
            .setContentText(title)
            .setAutoCancel(true)
            .build()
        notify(context, ("agenda$title$startMillis").hashCode(), notification)
    }

    /** Geen rechten of geen tijd herkend: laat de gebruiker de afspraak zelf opslaan. */
    fun notifyAddToCalendar(context: Context, title: String, startMillis: Long?) {
        ensureChannel(context)
        val pending = PendingIntent.getActivity(
            context, ("addcal$title").hashCode(), insertIntent(title, startMillis),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_reminder)
            .setContentTitle("Afspraak — tik om toe te voegen")
            .setContentText(title)
            .setStyle(NotificationCompat.BigTextStyle().bigText(title))
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()
        notify(context, ("addcal$title").hashCode(), notification)
    }

    private fun insertIntent(title: String, startMillis: Long?): Intent =
        Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, title)
            startMillis?.let {
                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, it)
                putExtra(CalendarContract.EXTRA_EVENT_END_TIME, it + DEFAULT_DURATION_MS)
            }
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

    private fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Agenda", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
    }

    private fun notify(context: Context, id: Int, notification: android.app.Notification) {
        val manager = NotificationManagerCompat.from(context)
        if (manager.areNotificationsEnabled()) {
            try {
                manager.notify(id, notification)
            } catch (e: SecurityException) {
                // POST_NOTIFICATIONS niet verleend.
            }
        }
    }
}
