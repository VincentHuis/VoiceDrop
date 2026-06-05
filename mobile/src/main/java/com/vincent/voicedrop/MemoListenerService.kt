package com.vincent.voicedrop

import android.util.Log
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Ontvangt memo's van het horloge via de Data Layer, verwerkt ze via [MemoProcessor] en ruimt
 * het afgeleverde DataItem op. Callbacks draaien op een achtergrondthread.
 */
class MemoListenerService : WearableListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onDataChanged(events: DataEventBuffer) {
        val dataClient = Wearable.getDataClient(this)

        for (event in events) {
            if (event.type != DataEvent.TYPE_CHANGED) continue
            val uri = event.dataItem.uri
            if (uri.path?.startsWith("/memo/") != true) continue

            val map = DataMapItem.fromDataItem(event.dataItem).dataMap
            val id = map.getString("id") ?: continue
            val rawText = map.getString("text").orEmpty()
            val timestamp = map.getLong("timestamp")

            scope.launch {
                try {
                    MemoProcessor(this@MemoListenerService).process(
                        id = id,
                        rawText = rawText,
                        timestamp = timestamp
                    )
                    dataClient.deleteDataItems(uri)
                } catch (t: Throwable) {
                    Log.e(TAG, "Failed to process memo $id; leaving DataItem for retry", t)
                }
            }
        }
    }

    companion object {
        private const val TAG = "MemoListenerService"
    }
}
