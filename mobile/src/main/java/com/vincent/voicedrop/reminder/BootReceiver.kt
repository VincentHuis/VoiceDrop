package com.vincent.voicedrop.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.vincent.voicedrop.data.MemoDatabase
import com.vincent.voicedrop.location.GeofenceManager

/** Alarmen en geofences worden gewist bij herstart; deze receiver zet ze opnieuw. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        Thread {
            try {
                val dao = MemoDatabase.get(context).memoDao()
                val now = System.currentTimeMillis()
                dao.upcomingRemindersNow(now).forEach { memo ->
                    ReminderScheduler.schedule(context, memo)
                }
                // Herhalende taken die tijdens 'uit' gepasseerd zijn: vooruitrollen en herplannen.
                dao.recurringPastNow(now).forEach { memo ->
                    val rule = memo.recurrence ?: return@forEach
                    val base = memo.remindAt ?: return@forEach
                    val nextAt = Recurrence.nextAfter(base, rule, now)
                    dao.setRemindAtNow(memo.id, nextAt)
                    ReminderScheduler.scheduleAt(context, memo.id, memo.text, nextAt)
                }
                GeofenceManager.registerAll(context)
            } finally {
                pending.finish()
            }
        }.start()
    }
}
