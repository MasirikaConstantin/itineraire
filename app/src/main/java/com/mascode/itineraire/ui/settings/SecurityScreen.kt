package com.mascode.itineraire.ui.settings

import android.content.Intent
import android.provider.Settings
import androidx.biometric.BiometricManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.mascode.itineraire.ui.auth.BiometricAuthenticator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityScreen(
    activity: FragmentActivity,
    biometricLockEnabled: Boolean,
    message: String?,
    onBack: () -> Unit,
    onProtectionChanged: (Boolean) -> Unit,
    onLockNow: () -> Unit,
    onAuthenticationError: (String) -> Unit,
) {
    val context = LocalContext.current
    val availability = remember { BiometricAuthenticator.availability(context) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Sécurité") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Retour")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (biometricLockEnabled) Icons.Outlined.Lock else Icons.Outlined.LockOpen,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Column(Modifier.padding(start = 16.dp)) {
                            Text(
                                if (biometricLockEnabled) "Protection active" else "Protection inactive",
                                style = MaterialTheme.typography.titleLarge,
                            )
                            Text(
                                if (biometricLockEnabled) {
                                    "L'application se verrouille lorsqu'elle passe en arrière-plan."
                                } else {
                                    "L'application reste accessible sans authentification."
                                },
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Text(
                        "La protection utilise l'empreinte, le visage compatible ou le code de verrouillage géré par Android. Elle ne nécessite pas de profil local.",
                        style = MaterialTheme.typography.bodyMedium,
                    )

                    message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }

                    if (availability == BiometricManager.BIOMETRIC_SUCCESS) {
                        Button(
                            onClick = {
                                val enabling = !biometricLockEnabled
                                BiometricAuthenticator.authenticate(
                                    activity = activity,
                                    title = if (enabling) "Activer la protection" else "Désactiver la protection",
                                    subtitle = "Confirmez votre identité avec la sécurité du téléphone",
                                    onSuccess = { onProtectionChanged(enabling) },
                                    onError = onAuthenticationError,
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(
                                if (biometricLockEnabled) Icons.Outlined.LockOpen else Icons.Outlined.Shield,
                                contentDescription = null,
                            )
                            Text(
                                if (biometricLockEnabled) "  Désactiver la protection" else "  Activer la protection",
                            )
                        }
                    } else {
                        Text(
                            BiometricAuthenticator.availabilityMessage(availability),
                            color = MaterialTheme.colorScheme.error,
                        )
                        TextButton(onClick = { context.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS)) }) {
                            Text("Ouvrir les réglages de sécurité")
                        }
                    }

                    if (biometricLockEnabled) {
                        OutlinedButton(onClick = onLockNow, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Outlined.Fingerprint, contentDescription = null)
                            Text("  Verrouiller maintenant")
                        }
                    }
                }
            }
        }
    }
}
