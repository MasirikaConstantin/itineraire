package com.mascode.itineraire.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mascode.itineraire.data.repository.BackupRepository
import com.mascode.itineraire.data.repository.BackupSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BackupUiState(
    val isWorking: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false,
)

class BackupViewModel(private val repository: BackupRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(BackupUiState())
    val state = mutableState.asStateFlow()

    fun export(uri: Uri) = runOperation("Sauvegarde créée") { repository.exportTo(uri) }

    fun restore(uri: Uri) = runOperation("Données restaurées") { repository.restoreFrom(uri) }

    fun clearMessage() {
        mutableState.value = BackupUiState()
    }

    private fun runOperation(label: String, operation: suspend () -> BackupSummary) {
        viewModelScope.launch {
            mutableState.value = BackupUiState(isWorking = true)
            runCatching { operation() }
                .onSuccess { summary ->
                    mutableState.value = BackupUiState(
                        message = "$label : ${summary.places} lieux, ${summary.days} journées, " +
                            "${summary.events} événements et ${summary.journeys} trajets.",
                    )
                }
                .onFailure { error ->
                    mutableState.value = BackupUiState(
                        message = error.message ?: "L'opération a échoué.",
                        isError = true,
                    )
                }
        }
    }
}
