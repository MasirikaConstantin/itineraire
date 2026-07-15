package com.mascode.itineraire.ui.places

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mascode.itineraire.data.local.entity.PlaceEntity
import com.mascode.itineraire.data.repository.PlaceRepository
import com.mascode.itineraire.domain.model.PlaceCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PlacesUiState(
    val places: List<PlaceEntity> = emptyList(),
    val errorMessage: String? = null,
)

class PlacesViewModel(private val repository: PlaceRepository) : ViewModel() {
    private val errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<PlacesUiState> = combine(repository.places, errorMessage) { places, error ->
        PlacesUiState(places, error)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlacesUiState())

    fun addPlace(
        name: String,
        category: PlaceCategory,
        latitude: Double? = null,
        longitude: Double? = null,
        onSaved: () -> Unit = {},
    ) {
        if (name.isBlank()) return
        viewModelScope.launch {
            runCatching { repository.add(name, category, latitude, longitude) }
                .onSuccess { onSaved() }
                .onFailure { errorMessage.value = "Ce lieu existe déjà ou n'est pas valide." }
        }
    }

    fun clearError() {
        errorMessage.value = null
    }
}
