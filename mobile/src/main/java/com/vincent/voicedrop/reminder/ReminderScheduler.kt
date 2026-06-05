package com.vincent.voicedrop.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.vincent.voicedrop.data.Memo

object ReminderScheduler {

    private fun pendingIntent(context: Context, memoId: String, text: String, create: Boolean): PendingIntent? {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("memoId", memoId)
            putExtra("text", text)
        }
        val flags = if (create) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
        }
        return PendingIntent.getBroadcast(context, memoId.hashCode(), intent, flags)
    }

    fun schedule(context: Context, memo: Memo) {
        val at = memo.remindAt ?: return
        scheduleAt(context, memo.id, memo.text, at)
    }

    /** Plant een alarm zonder volledig [Memo] (gebruikt door de snooze-actie). */
    fun scheduleAt(context: Context, memoId: String, text: String, at: Long) {
        val manager = context.getSystemService(AlarmManager::class.java)
        val pending = pendingIntent(context, memoId, text, create = true) ?: return
        try {
            val canExact = Build.VERSION.SDK_INT < 31 || manager.canScheduleExactAlarms()
            if (canExact) {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
            } else {
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
            }
        } catch (e: SecurityException) {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
        }
    }

    fun cancel(context: Context, memoId: String) {
        val pending = pendingIntent(context, memoId, "", create = false) ?: return
        context.getSystemService(AlarmManager::class.java).cancel(pending)
        pending.cancel()
    }
}
