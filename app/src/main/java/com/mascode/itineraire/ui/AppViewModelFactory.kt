package com.mascode.itineraire.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mascode.itineraire.AppContainer
import com.mascode.itineraire.ui.history.HistoryViewModel
import com.mascode.itineraire.ui.journey.ActiveJourneyViewModel
import com.mascode.itineraire.ui.journey.IncompleteLegsViewModel
import com.mascode.itineraire.ui.places.PlacesViewModel
import com.mascode.itineraire.ui.today.TodayViewModel
import com.mascode.itineraire.ui.settings.BackupViewModel

class AppViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    fun activeJourneyFactory(journeyId: String): ViewModelProvider.Factory =
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(ActiveJourneyViewModel::class.java))
                return ActiveJourneyViewModel(
                    journeyId = journeyId,
                    journeyRepository = container.journeyRepository,
                    placeRepository = container.placeRepository,
                    journeyNotificationManager = container.journeyNotificationManager,
                ) as T
            }
        }

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(AppViewModel::class.java) ->
            AppViewModel(
                container.localAccountRepository,
                container.appSecurityRepository,
                container.themeRepository,
                container.journeyNotificationManager,
            ) as T

        modelClass.isAssignableFrom(TodayViewModel::class.java) -> TodayViewModel(
            container.dayRepository,
            container.placeRepository,
            container.journeyRepository,
            container.quickActionRepository,
        ) as T

        modelClass.isAssignableFrom(PlacesViewModel::class.java) ->
            PlacesViewModel(container.placeRepository) as T

        modelClass.isAssignableFrom(HistoryViewModel::class.java) -> HistoryViewModel(
            container.journeyRepository,
            container.placeRepository,
        ) as T

        modelClass.isAssignableFrom(IncompleteLegsViewModel::class.java) -> IncompleteLegsViewModel(
            container.journeyRepository,
            container.placeRepository,
        ) as T

        modelClass.isAssignableFrom(BackupViewModel::class.java) ->
            BackupViewModel(container.backupRepository) as T

        else -> error("ViewModel inconnu : ${modelClass.name}")
    }
}
