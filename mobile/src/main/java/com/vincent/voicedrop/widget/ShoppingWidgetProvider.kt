package com.vincent.voicedrop.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.vincent.voicedrop.MainActivity
import com.vincent.voicedrop.R

class ShoppingWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (id in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_shopping)

            val serviceIntent = Intent(context, ShoppingWidgetService::class.java).apply {
                // Uniek maken per widget-id zodat elke widget zijn eigen adapter krijgt.
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            views.setRemoteAdapter(R.id.widget_list, serviceIntent)
            views.setEmptyView(R.id.widget_list, R.id.widget_empty)

            // Kop opent het Boodschappen-scherm in de app.
            val openIntent = Intent(context, MainActivity::class.java).apply {
                putExtra("openShopping", true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openPending = PendingIntent.getActivity(
                context, 0, openIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setOnClickPendingIntent(R.id.widget_header, openPending)

            // Tikken op een item opent ook de app (template + lege fill-in intent).
            val templateIntent = Intent(context, MainActivity::class.java).apply {
                putExtra("openShopping", true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val templatePending = PendingIntent.getActivity(
                context, 1, templateIntent,
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setPendingIntentTemplate(R.id.widget_list, templatePending)

            appWidgetManager.notifyAppWidgetViewDataChanged(id, R.id.widget_list)
            appWidgetManager.updateAppWidget(id, views)
        }
    }
}
