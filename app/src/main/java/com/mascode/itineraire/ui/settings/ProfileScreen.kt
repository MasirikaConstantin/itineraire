package com.mascode.itineraire.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.mascode.itineraire.data.local.entity.LocalAccountEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    account: LocalAccountEntity?,
    message: String?,
    onBack: () -> Unit,
    onSave: (String) -> Unit,
    onDelete: () -> Unit,
    onClearMessage: () -> Unit,
) {
    var name by remember(account?.displayName) { mutableStateOf(account?.displayName.orEmpty()) }
    var confirmDelete by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val nameFocusRequester = remember { FocusRequester() }

    LaunchedEffect(account) {
        if (account == null) {
            nameFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Profil local") },
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
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AccountCircle,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        if (account == null) "Créer un profil facultatif" else "Mettre à jour le profil",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        "Ce nom reste uniquement sur votre téléphone.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(20.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            onClearMessage()
                        },
                        modifier = Modifier.fillMaxWidth().focusRequester(nameFocusRequester),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        label = { Text("Votre nom") },
                        singleLine = true,
                    )
                    message?.let {
                        Spacer(Modifier.height(12.dp))
                        Text(it, color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { onSave(name) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = name.trim().length >= 2 && name.trim() != account?.displayName,
                    ) {
                        Icon(Icons.Outlined.Save, contentDescription = null)
                        Text("  Enregistrer")
                    }

                    if (account != null) {
                        Spacer(Modifier.height(12.dp))
                        if (confirmDelete) {
                            Text(
                                "Confirmez la suppression du profil. Vos trajets ne seront pas supprimés.",
                                color = MaterialTheme.colorScheme.error,
                            )
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    onDelete()
                                    confirmDelete = false
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Confirmer la suppression")
                            }
                            OutlinedButton(
                                onClick = { confirmDelete = false },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Annuler")
                            }
                        } else {
                            OutlinedButton(
                                onClick = { confirmDelete = true },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Supprimer le profil local")
                            }
                        }
                    }
                }
            }
        }
    }
}
