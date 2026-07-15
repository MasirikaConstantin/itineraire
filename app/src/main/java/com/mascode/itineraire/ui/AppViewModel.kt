package com.mascode.itineraire.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mascode.itineraire.data.local.entity.LocalAccountEntity
import com.mascode.itineraire.data.repository.AppSecurityRepository
import com.mascode.itineraire.data.repository.LocalAccountRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface AppAccessState {
    data object Loading : AppAccessState
    data class Locked(
        val account: LocalAccountEntity?,
        val errorMessage: String? = null,
    ) : AppAccessState

    data class Authenticated(
        val account: LocalAccountEntity?,
        val biometricLockEnabled: Boolean,
        val message: String? = null,
    ) : AppAccessState
}

class AppViewModel(
    private val accountRepository: LocalAccountRepository,
    private val securityRepository: AppSecurityRepository,
) : ViewModel() {
    private val authenticated = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)

    val accessState: StateFlow<AppAccessState> = combine(
        accountRepository.account,
        securityRepository.biometricLockEnabled,
        authenticated,
        message,
    ) { account, biometricLockEnabled, isAuthenticated, currentMessage ->
        when {
            !biometricLockEnabled || isAuthenticated -> AppAccessState.Authenticated(
                account = account,
                biometricLockEnabled = biometricLockEnabled,
                message = currentMessage,
            )
            else -> AppAccessState.Locked(account, currentMessage)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppAccessState.Loading)

    fun saveProfile(displayName: String) {
        viewModelScope.launch {
            runCatching { accountRepository.save(displayName) }
                .onSuccess { message.value = "Profil local enregistré." }
                .onFailure {
                    message.value = it.message ?: "Impossible d'enregistrer le profil local."
                }
        }
    }

    fun setBiometricLockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            runCatching {
                if (enabled) authenticated.value = true
                securityRepository.setBiometricLockEnabled(enabled)
            }.onSuccess {
                message.value = if (enabled) {
                    "Protection biométrique activée."
                } else {
                    "Protection biométrique désactivée."
                }
            }.onFailure {
                message.value = it.message ?: "Impossible de modifier la protection."
            }
        }
    }

    fun unlock() {
        message.value = null
        authenticated.value = true
    }

    fun lock() {
        message.value = null
        authenticated.value = false
    }

    fun reportAuthenticationError(error: String) {
        message.value = error
    }

    fun clearMessage() {
        message.value = null
    }
}
