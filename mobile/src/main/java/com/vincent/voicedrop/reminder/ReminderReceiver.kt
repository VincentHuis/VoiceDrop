package com.vincent.voicedrop.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.vincent.voicedrop.data.MemoDatabase

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val memoId = intent.getStringExtra("memoId") ?: return
        val text = intent.getStringExtra("text").orEmpty()

        val snoozeMinutes = intent.getIntExtra("snoozeMinutes", 0)
        if (snoozeMinutes > 0) {
            snooze(context, memoId, text, snoozeMinutes)
            return
        }

        ReminderNotifications.notifyReminder(context, memoId, text)
    }

    /** Sluit de melding, plant de herinnering [minutes] later opnieuw en bewaart de nieuwe tijd. */
    private fun snooze(context: Context, memoId: String, text: String, minutes: Int) {
        val newAt = System.currentTimeMillis() + minutes * 60_000L
        ReminderNotifications.cancelReminder(context, memoId)
        ReminderScheduler.scheduleAt(context, memoId, text, newAt)
        val pending = goAsync()
        Thread {
            try {
                MemoDatabase.get(context).memoDao().setRemindAtNow(memoId, newAt)
            } finally {
                pending.finish()
            }
        }.start()
    }
}
