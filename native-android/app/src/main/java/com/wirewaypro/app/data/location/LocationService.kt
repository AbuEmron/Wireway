package com.wirewaypro.app.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.wirewaypro.app.domain.model.LocationArea
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Resolves the device's current location to a human "City, ST" area, used to
 * localize AI pricing and the material pull list. Everything is best-effort and
 * returns null when location is unavailable or the runtime permission hasn't been
 * granted — callers fall back to a typed address. The runtime permission request
 * itself is driven by the UI; this just reads location once it's allowed.
 */
@Singleton
class LocationService @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val fused by lazy { LocationServices.getFusedLocationProviderClient(context) }

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    /** Current location reverse-geocoded to a "City, ST" area, or null. */
    suspend fun currentArea(): LocationArea? {
        if (!hasPermission()) return null
        val location = runCatching { requestLocation() }.getOrNull() ?: return null
        val label = withContext(Dispatchers.IO) { reverseGeocode(location.latitude, location.longitude) }
        return LocationArea(
            label = label ?: "your area",
            latitude = location.latitude,
            longitude = location.longitude,
        )
    }

    @SuppressLint("MissingPermission") // guarded by hasPermission()
    private suspend fun requestLocation(): Location? = suspendCancellableCoroutine { cont ->
        val cts = CancellationTokenSource()
        fused.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
            .addOnSuccessListener { loc -> if (cont.isActive) cont.resume(loc) }
            .addOnFailureListener { if (cont.isActive) cont.resume(null) }
        cont.invokeOnCancellation { cts.cancel() }
    }

    @Suppress("DEPRECATION") // the async Geocoder API is API 33+; the sync call runs off the main thread
    private fun reverseGeocode(lat: Double, lng: Double): String? = runCatching {
        Geocoder(context, Locale.US).getFromLocation(lat, lng, 1)
            ?.firstOrNull()
            ?.let { addr ->
                val city = addr.locality ?: addr.subAdminArea
                val state = addr.adminArea
                listOfNotNull(city, state).joinToString(", ").ifBlank { null }
            }
    }.getOrNull()
}
