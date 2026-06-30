package com.wirewaypro.app.domain.model

/**
 * A resolved job locale used to localize AI pricing and the material pull list.
 * [label] is a human area like "Binghamton, NY" (reverse-geocoded from GPS) or
 * whatever address the user typed. Coordinates are present only for GPS results.
 */
data class LocationArea(
    val label: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
)
