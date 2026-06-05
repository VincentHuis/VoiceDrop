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
                dao.upcomingRemindersNow(System.currentTimeMillis()).forEach { memo ->
                    ReminderScheduler.schedule(context, memo)
                }
                GeofenceManager.registerAll(context)
            } finally {
                pending.finish()
            }
        }.start()
    }
}
