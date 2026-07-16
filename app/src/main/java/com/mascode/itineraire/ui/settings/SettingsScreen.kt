package com.mascode.itineraire.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Widgets
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
import com.mascode.itineraire.data.local.entity.LocalAccountEntity
import com.mascode.itineraire.domain.model.ThemeMode

@Composable
fun SettingsScreen(
    account: LocalAccountEntity?,
    biometricLockEnabled: Boolean,
    themeMode: ThemeMode,
    onOpenProfile: () -> Unit,
    onOpenSecurity: () -> Unit,
    onOpenTheme: () -> Unit,
    onOpenBackup: () -> Unit,
    onAddWidget: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            Text("Paramètres", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Gérez le profil, la sécurité et les préférences de l'application.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        item {
            SectionTitle("Compte et sécurité")
            Card(Modifier.fillMaxWidth()) {
                SettingsRow(
                    icon = Icons.Outlined.AccountCircle,
                    title = "Profil local",
                    description = account?.displayName ?: "Aucun profil — facultatif",
                    onClick = onOpenProfile,
                )
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                SettingsRow(
                    icon = Icons.Outlined.Security,
                    title = "Sécurité et authentification",
                    description = if (biometricLockEnabled) "Protection active" else "Protection inactive",
                    onClick = onOpenSecurity,
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
                    description = themeMode.description(),
                    onClick = onOpenTheme,
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
                    description = "Exporter ou restaurer toutes les données",
                    onClick = onOpenBackup,
                )
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                SettingsRow(
                    icon = Icons.Outlined.Widgets,
                    title = "Widget du trajet",
                    description = "Ajouter le trajet en cours à l'écran d'accueil",
                    onClick = onAddWidget,
                )
            }
        }

        item {
            SectionTitle("À propos")
            Card(Modifier.fillMaxWidth()) {
                SettingsRow(
                    icon = Icons.Outlined.Info,
                    title = "Itinéraire",
                    description = "Version 1.3.0",
                )
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                SettingsRow(
                    icon = Icons.Outlined.PrivacyTip,
                    title = "Politique de confidentialité",
                    description = "Comprendre l'utilisation de vos données",
                    onClick = onOpenPrivacyPolicy,
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

private fun ThemeMode.description(): String = when (this) {
    ThemeMode.SYSTEM -> "Selon le thème du téléphone"
    ThemeMode.LIGHT -> "Clair"
    ThemeMode.DARK -> "Sombre"
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
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: (() -> Unit)? = null,
) {
    ListItem(
        modifier = if (onClick == null) Modifier else Modifier.clickable(onClick = onClick),
        headlineContent = { Text(title) },
        supportingContent = { Text(description) },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        trailingContent = onClick?.let {
            {
                Icon(Icons.Outlined.ChevronRight, contentDescription = "Ouvrir")
            }
        },
    )
}
