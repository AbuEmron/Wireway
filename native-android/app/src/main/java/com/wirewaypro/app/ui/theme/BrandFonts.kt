package com.wirewaypro.app.ui.theme

import android.content.Context
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

/**
 * Resolves the brand font (Poppins) from res/font at runtime *if present*, so the
 * app compiles and runs without the .ttf binaries and automatically upgrades the
 * moment they're dropped in (see native-android/FONTS.md).
 *
 * Poppins is the single brand typeface (display + body) for the "Powered by
 * Precision" identity — geometric, friendly, and legible at small sizes. The
 * legacy Space Grotesk / Inter resources are still picked up as a fallback if
 * Poppins is missing, so the app never renders bare.
 */
object BrandFonts {

    data class Families(val display: FontFamily?, val body: FontFamily?)

    fun resolve(context: Context): Families {
        val poppins = familyOrNull(
            context,
            listOf(
                "poppins_regular" to FontWeight.Normal,
                "poppins_medium" to FontWeight.Medium,
                "poppins_semibold" to FontWeight.SemiBold,
                "poppins_bold" to FontWeight.Bold,
            ),
        )
        // Legacy fallbacks (kept so the app still renders if Poppins is absent).
        val legacyDisplay = familyOrNull(
            context,
            listOf(
                "space_grotesk_regular" to FontWeight.Normal,
                "space_grotesk_medium" to FontWeight.Medium,
                "space_grotesk_semibold" to FontWeight.SemiBold,
                "space_grotesk_bold" to FontWeight.Bold,
            ),
        )
        val legacyBody = familyOrNull(
            context,
            listOf(
                "inter_regular" to FontWeight.Normal,
                "inter_medium" to FontWeight.Medium,
                "inter_semibold" to FontWeight.SemiBold,
            ),
        )
        return Families(
            display = poppins ?: legacyDisplay,
            body = poppins ?: legacyBody,
        )
    }

    private fun familyOrNull(context: Context, specs: List<Pair<String, FontWeight>>): FontFamily? {
        val fonts = specs.mapNotNull { (name, weight) ->
            val id = context.resources.getIdentifier(name, "font", context.packageName)
            if (id != 0) Font(id, weight) else null
        }
        return if (fonts.isNotEmpty()) FontFamily(fonts) else null
    }
}
