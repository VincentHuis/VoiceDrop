package com.vincent.polsnotitie

import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.runBlocking

/**
 * Ontvangt memo's van het horloge via de Data Layer, verwerkt ze via [MemoProcessor] en ruimt
 * het afgeleverde DataItem op. Callbacks draaien op een achtergrondthread.
 */
class MemoListenerService : WearableListenerService() {

    override fun onDataChanged(events: DataEventBuffer) {
        val dataClient = Wearable.getDataClient(this)

        for (event in events) {
            if (event.type != DataEvent.TYPE_CHANGED) continue
            val uri = event.dataItem.uri
            if (uri.path?.startsWith("/memo/") != true) continue

            val map = DataMapItem.fromDataItem(event.dataItem).dataMap
            val id = map.getString("id") ?: continue

            runBlocking {
                MemoProcessor.process(
                    context = this@MemoListenerService,
                    id = id,
                    rawText = map.getString("text").orEmpty(),
                    timestamp = map.getLong("timestamp")
                )
            }
            dataClient.deleteDataItems(uri)
        }
    }
}
