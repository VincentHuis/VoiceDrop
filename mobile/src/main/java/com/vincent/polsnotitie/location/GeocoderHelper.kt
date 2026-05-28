package com.vincent.polsnotitie.location

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.osmdroid.util.GeoPoint
import java.util.Locale
import kotlin.coroutines.resume

/** Adres <-> coördinaten via de ingebouwde Android Geocoder (geen API-key). */
object GeocoderHelper {

    /** Zoekt coördinaten bij een ingetypt adres ("Kraneweg 10 Groningen"). */
    suspend fun search(context: Context, query: String): GeoPoint? = withContext(Dispatchers.IO) {
        if (query.isBlank() || !Geocoder.isPresent()) return@withContext null
        val geocoder = Geocoder(context, Locale.getDefault())
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { cont ->
                    geocoder.getFromLocationName(query, 1, object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) {
                            val a = addresses.firstOrNull()
                            cont.resume(a?.let { GeoPoint(it.latitude, it.longitude) })
                        }

                        override fun onError(errorMessage: String?) = cont.resume(null)
                    })
                }
            } else {
                @Suppress("DEPRECATION")
                geocoder.getFromLocationName(query, 1)?.firstOrNull()
                    ?.let { GeoPoint(it.latitude, it.longitude) }
            }
        } catch (e: Exception) {
            null
        }
    }

    /** Zoekt een leesbaar adres bij coördinaten. */
    suspend fun addressOf(context: Context, lat: Double, lng: Double): String? =
        withContext(Dispatchers.IO) {
            if (!Geocoder.isPresent()) return@withContext null
            val geocoder = Geocoder(context, Locale.getDefault())
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    suspendCancellableCoroutine { cont ->
                        geocoder.getFromLocation(lat, lng, 1, object : Geocoder.GeocodeListener {
                            override fun onGeocode(addresses: MutableList<Address>) {
                                cont.resume(addresses.firstOrNull()?.let { format(it) })
                            }

                            override fun onError(errorMessage: String?) = cont.resume(null)
                        })
                    }
                } else {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(lat, lng, 1)?.firstOrNull()?.let { format(it) }
                }
            } catch (e: Exception) {
                null
            }
        }

    private fun format(a: Address): String {
        a.getAddressLine(0)?.let { return it }
        return listOfNotNull(
            listOfNotNull(a.thoroughfare, a.subThoroughfare).joinToString(" ").ifBlank { null },
            a.locality
        ).joinToString(", ")
    }
}
