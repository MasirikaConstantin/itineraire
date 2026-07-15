package com.mascode.itineraire.domain.geo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class HaversineTest {
    @Test
    fun `une position identique produit une distance nulle`() {
        val distance = haversineDistanceMeters(-4.325, 15.322, -4.325, 15.322)

        assertEquals(0.0, distance, 0.001)
    }

    @Test
    fun `la distance est symetrique`() {
        val outward = haversineDistanceMeters(-4.325, 15.322, -4.404, 15.287)
        val returnTrip = haversineDistanceMeters(-4.404, 15.287, -4.325, 15.322)

        assertEquals(outward, returnTrip, 0.001)
    }

    @Test
    fun `un degre de longitude a equateur vaut environ 111 kilometres`() {
        val distance = haversineDistanceMeters(0.0, 0.0, 0.0, 1.0)

        assertEquals(111_195.0, distance, 1.0)
    }

    @Test
    fun `une latitude invalide est refusee`() {
        assertThrows(IllegalArgumentException::class.java) {
            haversineDistanceMeters(91.0, 15.322, -4.325, 15.322)
        }
    }
}
