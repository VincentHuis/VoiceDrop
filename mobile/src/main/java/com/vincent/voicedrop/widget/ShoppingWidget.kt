package com.vincent.voicedrop.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import com.vincent.voicedrop.R

/** Helper om de boodschappen-widget(s) te laten verversen na een datawijziging. */
object ShoppingWidget {
    fun refresh(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(
            ComponentName(context, ShoppingWidgetProvider::class.java)
        )
        if (ids.isNotEmpty()) {
            manager.notifyAppWidgetViewDataChanged(ids, R.id.widget_list)
        }
    }
}
