package com.vincent.voicedrop.widget

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.vincent.voicedrop.R
import com.vincent.voicedrop.data.Category
import com.vincent.voicedrop.data.Memo
import com.vincent.voicedrop.data.MemoDatabase

class ShoppingWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory =
        ShoppingRemoteViewsFactory(applicationContext)
}

private class ShoppingRemoteViewsFactory(
    private val context: Context
) : RemoteViewsService.RemoteViewsFactory {

    private var items: List<Memo> = emptyList()

    override fun onCreate() {}

    override fun onDataSetChanged() {
        items = MemoDatabase.get(context).memoDao()
            .byCategoryUncheckedNow(Category.BOODSCHAPPEN.name)
    }

    override fun onDestroy() {
        items = emptyList()
    }

    override fun getCount(): Int = items.size

    override fun getViewAt(position: Int): RemoteViews {
        val memo = items[position]
        return RemoteViews(context.packageName, R.layout.widget_item).apply {
            setTextViewText(R.id.widget_item_text, memo.text)
            setOnClickFillInIntent(R.id.widget_item_text, Intent())
        }
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = items[position].id.hashCode().toLong()

    override fun hasStableIds(): Boolean = true
}
