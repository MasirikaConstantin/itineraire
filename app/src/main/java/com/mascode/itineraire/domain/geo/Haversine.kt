package com.mascode.itineraire.domain.geo

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private const val EARTH_MEAN_RADIUS_METERS = 6_371_008.8

/** Calcule la distance orthodromique entre deux coordonnées géographiques. */
fun haversineDistanceMeters(
    startLatitude: Double,
    startLongitude: Double,
    endLatitude: Double,
    endLongitude: Double,
): Double {
    require(startLatitude in -90.0..90.0 && endLatitude in -90.0..90.0) {
        "Latitude invalide."
    }
    require(startLongitude in -180.0..180.0 && endLongitude in -180.0..180.0) {
        "Longitude invalide."
    }

    val startLatitudeRadians = Math.toRadians(startLatitude)
    val endLatitudeRadians = Math.toRadians(endLatitude)
    val latitudeDelta = endLatitudeRadians - startLatitudeRadians
    val longitudeDelta = Math.toRadians(endLongitude - startLongitude)
    val haversine = (
        sin(latitudeDelta / 2) * sin(latitudeDelta / 2) +
            cos(startLatitudeRadians) * cos(endLatitudeRadians) *
            sin(longitudeDelta / 2) * sin(longitudeDelta / 2)
        ).coerceIn(0.0, 1.0)
    val centralAngle = 2 * atan2(sqrt(haversine), sqrt(1 - haversine))

    return EARTH_MEAN_RADIUS_METERS * centralAngle
}
