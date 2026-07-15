package com.mascode.itineraire.data.repository

import com.mascode.itineraire.data.local.dao.PlaceDao
import com.mascode.itineraire.data.local.entity.PlaceEntity
import com.mascode.itineraire.domain.model.PlaceCategory

class PlaceRepository(private val placeDao: PlaceDao) {
    val places = placeDao.observeAll()

    suspend fun add(name: String, category: PlaceCategory) {
        placeDao.insert(PlaceEntity(name = name.trim(), category = category))
    }

    suspend fun delete(place: PlaceEntity) = placeDao.delete(place)
}
