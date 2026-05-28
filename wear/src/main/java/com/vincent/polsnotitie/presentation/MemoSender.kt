package com.vincent.polsnotitie.presentation

import android.content.Context
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import java.util.UUID

/**
 * Schrijft een memo als DataItem op een uniek pad `/memo/<uuid>`. De Data Layer bewaart
 * het lokaal en synct het vanzelf naar de telefoon zodra die in bereik is.
 */
object MemoSender {
    fun send(context: Context, text: String, onResult: (Boolean) -> Unit) {
        val id = UUID.randomUUID().toString()
        val request = PutDataMapRequest.create("/memo/$id").apply {
            dataMap.putString("id", id)
            dataMap.putString("text", text)
            dataMap.putLong("timestamp", System.currentTimeMillis())
        }.asPutDataRequest().setUrgent()

        Wearable.getDataClient(context).putDataItem(request)
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }
}
