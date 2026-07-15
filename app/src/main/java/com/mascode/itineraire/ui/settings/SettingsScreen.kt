package com.mascode.itineraire.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            Text("Paramètres", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Gérez les préférences, les données et le futur compte de l'application.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        item {
            SectionTitle("Compte")
            Card(Modifier.fillMaxWidth()) {
                SettingsRow(
                    icon = Icons.Outlined.AccountCircle,
                    title = "Aucun compte connecté",
                    description = "Vos données restent uniquement sur ce téléphone.",
                )
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                Text(
                    text = "La connexion et la synchronisation seront ajoutées avec la sauvegarde en ligne.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            SectionTitle("Application")
            Card(Modifier.fillMaxWidth()) {
                SettingsRow(
                    icon = Icons.Outlined.Payments,
                    title = "Devise",
                    description = "Franc congolais (CDF)",
                )
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                SettingsRow(
                    icon = Icons.Outlined.DarkMode,
                    title = "Thème",
                    description = "Selon le thème du téléphone",
                )
            }
        }

        item {
            SectionTitle("Données")
            Card(Modifier.fillMaxWidth()) {
                SettingsRow(
                    icon = Icons.Outlined.Storage,
                    title = "Stockage local",
                    description = "Base de données locale active",
                )
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                SettingsRow(
                    icon = Icons.Outlined.Backup,
                    title = "Sauvegarde et restauration",
                    description = "Fonctionnalité à venir",
                )
            }
        }

        item {
            SectionTitle("À propos")
            Card(Modifier.fillMaxWidth()) {
                SettingsRow(
                    icon = Icons.Outlined.Info,
                    title = "Itinéraire",
                    description = "Version 1.0",
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        modifier = Modifier.padding(bottom = 6.dp),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun SettingsRow(icon: ImageVector, title: String, description: String) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(description) },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
    )
}
