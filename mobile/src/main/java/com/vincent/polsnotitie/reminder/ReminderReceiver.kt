package com.vincent.polsnotitie.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val memoId = intent.getStringExtra("memoId") ?: return
        val text = intent.getStringExtra("text").orEmpty()
        ReminderNotifications.notifyReminder(context, memoId, text)
    }
}
