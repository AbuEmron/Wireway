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
import com.wirewaypro.app.domain.ahj.UsStates
import com.wirewaypro.app.domain.model.LocationArea
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
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

    /**
     * Current location reverse-geocoded to a "City, ST" area, or null.
     *
     * Requests a FRESH high-accuracy fix each call so it tracks the device as it
     * moves (never a stale cached city). Only if that times out does it fall back
     * to the last-known location, which is labeled "(approx.)" so the user knows
     * it may be off.
     */
    suspend fun currentArea(): LocationArea? {
        if (!hasPermission()) return null
        val fresh = runCatching { freshLocation() }.getOrNull()
        val location = fresh ?: runCatching { lastKnownLocation() }.getOrNull() ?: return null
        val approximate = fresh == null
        val label = withContext(Dispatchers.IO) { reverseGeocode(location.latitude, location.longitude) }
        val base = label ?: "your area"
        return LocationArea(
            label = if (approximate) "$base (approx.)" else base,
            latitude = location.latitude,
            longitude = location.longitude,
        )
    }

    /**
     * A best-effort jurisdiction PRE-SUGGESTION from the device location — state
     * code (+ county/city when the geocoder provides them). Only ever a
     * suggestion: the caller shows it for the user to confirm or override, never a
     * silently-saved location. Returns null when location/permission is
     * unavailable or the state can't be resolved to a known code.
     */
    suspend fun suggestJurisdiction(): JurisdictionSuggestion? {
        if (!hasPermission()) return null
        val location = runCatching { freshLocation() }.getOrNull()
            ?: runCatching { lastKnownLocation() }.getOrNull()
            ?: return null
        return withContext(Dispatchers.IO) {
            runCatching {
                Geocoder(context, Locale.US).getFromLocation(location.latitude, location.longitude, 1)
                    ?.firstOrNull()
                    ?.let { addr ->
                        val stateCode = UsStates.codeForName(addr.adminArea) ?: return@let null
                        JurisdictionSuggestion(
                            stateCode = stateCode,
                            county = addr.subAdminArea,
                            city = addr.locality,
                        )
                    }
            }.getOrNull()
        }
    }

    /** A fresh high-accuracy GPS fix, or null if none arrives within the timeout. */
    @SuppressLint("MissingPermission") // guarded by hasPermission()
    private suspend fun freshLocation(): Location? = withTimeoutOrNull(FRESH_FIX_TIMEOUT_MS) {
        suspendCancellableCoroutine { cont ->
            val cts = CancellationTokenSource()
            fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                .addOnSuccessListener { loc -> if (cont.isActive) cont.resume(loc) }
                .addOnFailureListener { if (cont.isActive) cont.resume(null) }
            cont.invokeOnCancellation { cts.cancel() }
        }
    }

    /** Last-known cached fix — only used as a labeled fallback when a fresh fix times out. */
    @SuppressLint("MissingPermission") // guarded by hasPermission()
    private suspend fun lastKnownLocation(): Location? = suspendCancellableCoroutine { cont ->
        fused.lastLocation
            .addOnSuccessListener { loc -> if (cont.isActive) cont.resume(loc) }
            .addOnFailureListener { if (cont.isActive) cont.resume(null) }
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

    private companion object {
        /** How long to wait for a fresh GPS fix before falling back to last-known. */
        const val FRESH_FIX_TIMEOUT_MS = 10_000L
    }
}

/**
 * A location-derived jurisdiction PRE-SUGGESTION. Never authoritative — the user
 * always confirms or overrides it in the picker before it's saved.
 */
data class JurisdictionSuggestion(
    val stateCode: String,
    val county: String?,
    val city: String?,
)
