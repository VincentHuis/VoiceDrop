package com.vincent.polsnotitie.location

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.vincent.polsnotitie.data.MemoDatabase

/** Registreert geofences voor alle ingestelde plekken (trigger bij binnenkomen). */
object GeofenceManager {

    private fun hasLocationPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        return PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    @SuppressLint("MissingPermission")
    fun registerAll(context: Context) {
        if (!hasLocationPermission(context)) return
        val places = MemoDatabase.get(context).placeDao().getAllNow()
        val client = LocationServices.getGeofencingClient(context)

        client.removeGeofences(pendingIntent(context))
        if (places.isEmpty()) return

        val geofences = places.map { place ->
            Geofence.Builder()
                .setRequestId(place.id.toString())
                .setCircularRegion(place.lat, place.lng, place.radius)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .build()
        }
        val request = GeofencingRequest.Builder()
            .setInitialTrigger(0)
            .addGeofences(geofences)
            .build()

        try {
            client.addGeofences(request, pendingIntent(context))
        } catch (e: SecurityException) {
            // Achtergrond-locatie nog niet verleend; opnieuw proberen na toestemming.
        }
    }
}
