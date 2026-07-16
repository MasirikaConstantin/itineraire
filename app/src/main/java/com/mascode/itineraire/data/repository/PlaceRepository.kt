package com.mascode.itineraire.data.repository

import com.mascode.itineraire.data.local.dao.PlaceDao
import com.mascode.itineraire.data.local.entity.PlaceEntity
import com.mascode.itineraire.domain.model.PlaceCategory
import java.time.Instant

class PlaceRepository(private val placeDao: PlaceDao) {
    val places = placeDao.observeAll()

    suspend fun getAll(): List<PlaceEntity> = placeDao.getAll()

    suspend fun add(
        name: String,
        category: PlaceCategory,
        latitude: Double? = null,
        longitude: Double? = null,
    ) {
        require((latitude == null) == (longitude == null)) { "La position doit être complète." }
        latitude?.let { require(it in -90.0..90.0) { "Latitude invalide." } }
        longitude?.let { require(it in -180.0..180.0) { "Longitude invalide." } }
        placeDao.insert(
            PlaceEntity(
                name = name.trim(),
                category = category,
                latitude = latitude,
                longitude = longitude,
            ),
        )
    }

    suspend fun update(
        id: String,
        name: String,
        category: PlaceCategory,
        latitude: Double? = null,
        longitude: Double? = null,
    ) {
        require((latitude == null) == (longitude == null)) { "La position doit être complète." }
        latitude?.let { require(it in -90.0..90.0) { "Latitude invalide." } }
        longitude?.let { require(it in -180.0..180.0) { "Longitude invalide." } }
        val place = requireNotNull(placeDao.findById(id)) { "Lieu introuvable." }
        placeDao.update(
            place.copy(
                name = name.trim(),
                category = category,
                latitude = latitude,
                longitude = longitude,
                updatedAt = Instant.now(),
            ),
        )
    }

    suspend fun delete(place: PlaceEntity) = placeDao.delete(place)
}
