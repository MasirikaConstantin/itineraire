package com.mascode.itineraire.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mascode.itineraire.data.local.entity.LocalAccountEntity
import com.mascode.itineraire.data.repository.LocalAccountRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface AppAccessState {
    data object Loading : AppAccessState
    data class NeedsAccount(val errorMessage: String? = null) : AppAccessState
    data class Locked(
        val account: LocalAccountEntity,
        val errorMessage: String? = null,
    ) : AppAccessState

    data class Authenticated(val account: LocalAccountEntity) : AppAccessState
}

class AppViewModel(private val accountRepository: LocalAccountRepository) : ViewModel() {
    private val authenticated = MutableStateFlow(false)
    private val errorMessage = MutableStateFlow<String?>(null)

    val accessState: StateFlow<AppAccessState> = combine(
        accountRepository.account,
        authenticated,
        errorMessage,
    ) { account, isAuthenticated, error ->
        when {
            account == null -> AppAccessState.NeedsAccount(error)
            isAuthenticated -> AppAccessState.Authenticated(account)
            else -> AppAccessState.Locked(account, error)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppAccessState.Loading)

    fun createAccount(displayName: String) {
        viewModelScope.launch {
            runCatching { accountRepository.create(displayName) }
                .onSuccess {
                    errorMessage.value = null
                    authenticated.value = true
                }
                .onFailure {
                    errorMessage.value = it.message ?: "Impossible de créer le compte local."
                }
        }
    }

    fun unlock() {
        errorMessage.value = null
        authenticated.value = true
    }

    fun lock() {
        errorMessage.value = null
        authenticated.value = false
    }

    fun reportAuthenticationError(message: String) {
        errorMessage.value = message
    }

    fun clearError() {
        errorMessage.value = null
    }
}
