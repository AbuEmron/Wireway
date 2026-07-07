package com.wirewaypro.app.esign

import android.content.Context
import android.os.Build

/**
 * Captures the honest signing environment for the Completion Certificate + audit
 * trail: device model, app version, and a best-effort network address.
 *
 * IP note: V1 is in-person sign-on-device, so we record the device's own active
 * network address (enumerated locally). It is NOT a public/server-observed IP — a
 * future remote-signing path should record the server-observed address instead. We
 * never fabricate it: if no non-loopback address is found, it stays null and the
 * certificate prints "Not recorded".
 */
object SigningEnvironment {

    fun of(context: Context): SigningContext = SigningContext(
        deviceModel = deviceModel(),
        appVersion = appVersion(context),
        ipAddress = localIpAddress(),
    )

    private fun deviceModel(): String {
        val make = Build.MANUFACTURER?.replaceFirstChar { it.uppercase() }.orEmpty()
        val model = Build.MODEL.orEmpty()
        return listOf(make, model).filter { it.isNotBlank() }.joinToString(" ").ifBlank { "Android device" }
    }

    private fun appVersion(context: Context): String = runCatching {
        val pkg = context.packageManager.getPackageInfo(context.packageName, 0)
        pkg.versionName ?: "unknown"
    }.getOrDefault("unknown")

    private fun localIpAddress(): String? = runCatching {
        java.net.NetworkInterface.getNetworkInterfaces().toList()
            .flatMap { it.inetAddresses.toList() }
            .firstOrNull { addr ->
                !addr.isLoopbackAddress && addr is java.net.Inet4Address && !addr.hostAddress.isNullOrBlank()
            }
            ?.hostAddress
    }.getOrNull()
}
