package com.mascode.itineraire.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mascode.itineraire.AppContainer
import com.mascode.itineraire.ui.history.HistoryViewModel
import com.mascode.itineraire.ui.places.PlacesViewModel
import com.mascode.itineraire.ui.today.TodayViewModel

class AppViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(AppViewModel::class.java) ->
            AppViewModel(container.localAccountRepository) as T

        modelClass.isAssignableFrom(TodayViewModel::class.java) -> TodayViewModel(
            container.dayRepository,
            container.placeRepository,
            container.journeyRepository,
        ) as T

        modelClass.isAssignableFrom(PlacesViewModel::class.java) ->
            PlacesViewModel(container.placeRepository) as T

        modelClass.isAssignableFrom(HistoryViewModel::class.java) -> HistoryViewModel(
            container.journeyRepository,
            container.placeRepository,
        ) as T

        else -> error("ViewModel inconnu : ${modelClass.name}")
    }
}
