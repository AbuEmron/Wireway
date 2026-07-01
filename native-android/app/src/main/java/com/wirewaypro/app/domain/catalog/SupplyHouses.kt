package com.wirewaypro.app.domain.catalog

import java.net.URLEncoder

/**
 * Public US supply houses the Material Pull List sources from — big-box / MRO
 * stores with public online pricing, plus the national electrical distributors.
 * Ported from the web app's src/data/supply-houses.js (fix/material-pull-list-
 * supply-houses). [searchUrl] builds a best-effort product-search link; houses
 * without a public search URL fall back to a name-scoped web search.
 */
data class SupplyHouse(
    val id: String,
    val name: String,
    val label: String,
    val group: String,
) {
    fun searchUrl(query: String): String {
        val q = URLEncoder.encode(query, "UTF-8")
        return when (id) {
            "homedepot" -> "https://www.homedepot.com/s/$q"
            "lowes" -> "https://www.lowes.com/search?searchTerm=$q"
            "menards" -> "https://www.menards.com/main/search.html?search=$q"
            "grainger" -> "https://www.grainger.com/search?searchQuery=$q"
            "ferguson" -> "https://www.ferguson.com/search/$q"
            "graybar" -> "https://www.graybar.com/search?text=$q"
            "platt" -> "https://www.platt.com/search.aspx?q=$q"
            else -> "https://www.google.com/search?q=" + URLEncoder.encode("$name $query", "UTF-8")
        }
    }
}

object SupplyHouses {
    const val GROUP_BIG_BOX = "Big box"
    const val GROUP_DISTRIBUTORS = "Electrical distributors"

    val ALL: List<SupplyHouse> = listOf(
        // Big box / MRO (broadly stocked, walk-in, public online pricing)
        SupplyHouse("homedepot", "Home Depot", "Home Depot", GROUP_BIG_BOX),
        SupplyHouse("lowes", "Lowe's", "Lowe's", GROUP_BIG_BOX),
        SupplyHouse("menards", "Menards", "Menards", GROUP_BIG_BOX),
        SupplyHouse("grainger", "Grainger", "Grainger", GROUP_BIG_BOX),
        SupplyHouse("ferguson", "Ferguson", "Ferguson", GROUP_BIG_BOX),
        // National electrical distributors
        SupplyHouse("ced", "CED Consolidated Electrical Distributors", "CED", GROUP_DISTRIBUTORS),
        SupplyHouse("graybar", "Graybar", "Graybar", GROUP_DISTRIBUTORS),
        SupplyHouse("rexel", "Rexel USA", "Rexel", GROUP_DISTRIBUTORS),
        SupplyHouse("platt", "Platt Electric Supply", "Platt", GROUP_DISTRIBUTORS),
        SupplyHouse("wesco", "WESCO Anixter", "WESCO", GROUP_DISTRIBUTORS),
        SupplyHouse("borderstates", "Border States Electric", "Border States", GROUP_DISTRIBUTORS),
        SupplyHouse("cityelectric", "City Electric Supply", "City Electric", GROUP_DISTRIBUTORS),
        SupplyHouse("crawford", "Crawford Electric Supply", "Crawford", GROUP_DISTRIBUTORS),
        SupplyHouse("mayer", "Mayer Electric Supply", "Mayer", GROUP_DISTRIBUTORS),
        SupplyHouse("summit", "Summit Electric Supply", "Summit", GROUP_DISTRIBUTORS),
        SupplyHouse("elliott", "Elliott Electric Supply", "Elliott", GROUP_DISTRIBUTORS),
        SupplyHouse("gexpro", "Gexpro", "Gexpro", GROUP_DISTRIBUTORS),
    )

    val GROUPS: List<String> = listOf(GROUP_BIG_BOX, GROUP_DISTRIBUTORS)

    /** The live-priced big-box stores priced by default. */
    val DEFAULT_SOURCES: List<String> = listOf("homedepot", "lowes")

    fun byId(id: String): SupplyHouse? = ALL.firstOrNull { it.id == id }

    /** Comma-joined house names for the AI prompt. */
    fun promptList(): String = ALL.joinToString("; ") { "${it.name} (${it.group})" }
}
