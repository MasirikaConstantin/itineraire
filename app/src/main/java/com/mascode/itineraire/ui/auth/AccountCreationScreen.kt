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
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity

@Composable
fun AccountCreationScreen(
    activity: FragmentActivity,
    errorMessage: String?,
    onCreateAccount: (String) -> Unit,
    onAuthenticationError: (String) -> Unit,
    onClearError: () -> Unit,
) {
    val context = LocalContext.current
    val availability = remember { BiometricAuthenticator.availability(context) }
    var name by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Outlined.AccountCircle,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(20.dp))
        Text("Créer votre compte local", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "Ce profil est obligatoire et reste uniquement sur ce téléphone.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
                onClearError()
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Votre nom") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "La création sera confirmée avec une empreinte, un visage compatible ou le verrouillage du téléphone.",
            style = MaterialTheme.typography.bodySmall,
        )

        errorMessage?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(24.dp))
        if (availability == BiometricManager.BIOMETRIC_SUCCESS) {
            Button(
                onClick = {
                    BiometricAuthenticator.authenticate(
                        activity = activity,
                        title = "Créer le compte local",
                        subtitle = "Confirmez votre identité pour protéger Itinéraire",
                        onSuccess = { onCreateAccount(name) },
                        onError = onAuthenticationError,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.trim().length >= 2,
            ) {
                Icon(Icons.Outlined.Security, contentDescription = null)
                Text("  Créer et sécuriser le compte")
            }
        } else {
            Text(
                BiometricAuthenticator.availabilityMessage(availability),
                color = MaterialTheme.colorScheme.error,
            )
            TextButton(
                onClick = { context.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS)) },
            ) {
                Text("Ouvrir les réglages de sécurité")
            }
        }
    }
}
