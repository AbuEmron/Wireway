package com.wirewaypro.app.ui.theme

import android.content.Context
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

/**
 * Resolves the brand fonts (Space Grotesk display + Inter body) from res/font at
 * runtime *if present*, so the app compiles and runs without the .ttf binaries
 * and automatically upgrades the moment they're dropped in (see native-android/FONTS.md).
 */
object BrandFonts {

    data class Families(val display: FontFamily?, val body: FontFamily?)

    fun resolve(context: Context): Families = Families(
        display = familyOrNull(
            context,
            listOf(
                "space_grotesk_regular" to FontWeight.Normal,
                "space_grotesk_medium" to FontWeight.Medium,
                "space_grotesk_semibold" to FontWeight.SemiBold,
                "space_grotesk_bold" to FontWeight.Bold,
            ),
        ),
        body = familyOrNull(
            context,
            listOf(
                "inter_regular" to FontWeight.Normal,
                "inter_medium" to FontWeight.Medium,
                "inter_semibold" to FontWeight.SemiBold,
            ),
        ),
    )

    private fun familyOrNull(context: Context, specs: List<Pair<String, FontWeight>>): FontFamily? {
        val fonts = specs.mapNotNull { (name, weight) ->
            val id = context.resources.getIdentifier(name, "font", context.packageName)
            if (id != 0) Font(id, weight) else null
        }
        return if (fonts.isNotEmpty()) FontFamily(fonts) else null
    }
}
