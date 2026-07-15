package com.mascode.itineraire.ui.auth

import android.content.Intent
import android.provider.Settings
import androidx.biometric.BiometricManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity

@Composable
fun LockedScreen(
    activity: FragmentActivity,
    displayName: String,
    errorMessage: String?,
    onAuthenticated: () -> Unit,
    onAuthenticationError: (String) -> Unit,
) {
    val context = LocalContext.current
    val availability = remember { BiometricAuthenticator.availability(context) }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Outlined.Lock,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(20.dp))
        Text("Itinéraire est verrouillée", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text("Bonjour $displayName", style = MaterialTheme.typography.titleMedium)
        Text("Authentifiez-vous pour accéder à vos déplacements.")

        errorMessage?.let {
            Spacer(Modifier.height(16.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(24.dp))
        if (availability == BiometricManager.BIOMETRIC_SUCCESS) {
            Button(
                onClick = {
                    BiometricAuthenticator.authenticate(
                        activity = activity,
                        title = "Déverrouiller Itinéraire",
                        subtitle = "Utilisez votre empreinte, votre visage ou le code du téléphone",
                        onSuccess = onAuthenticated,
                        onError = onAuthenticationError,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Outlined.Fingerprint, contentDescription = null)
                Text("  Déverrouiller")
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
    }
}
