package com.mascode.itineraire.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Politique de confidentialité") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Retour",
                        )
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Icon(
                    imageVector = Icons.Outlined.PrivacyTip,
                    contentDescription = null,
                    modifier = Modifier.padding(top = 16.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "Votre historique de déplacement vous appartient.",
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = "Dernière mise à jour : 15 juillet 2026",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            item {
                PrivacySection(
                    title = "Données enregistrées",
                    content = "L'application enregistre les informations que vous saisissez : profil local facultatif, lieux, événements de la journée, trajets, tronçons, modes de transport, horaires, durées, coûts et observations. Les préférences de thème et de protection sont également conservées.",
                )
            }
            item {
                PrivacySection(
                    title = "Utilisation des données",
                    content = "Ces informations servent uniquement au fonctionnement de l'application : afficher votre historique, suivre vos déplacements et préparer de futures statistiques personnelles. Elles ne sont pas utilisées à des fins publicitaires.",
                )
            }
            item {
                PrivacySection(
                    title = "Stockage et transmission",
                    content = "Les données sont stockées dans l'espace privé de l'application sur votre téléphone. L'application ne possède actuellement aucun service en ligne, aucun outil d'analyse ou de publicité, et ne demande pas l'accès à Internet. Android peut néanmoins sauvegarder ou transférer les données de l'application selon les réglages du téléphone et du compte système.",
                )
            }
            item {
                PrivacySection(
                    title = "Localisation",
                    content = "Les lieux et les trajets sont saisis manuellement. La version actuelle ne demande pas l'autorisation de localisation et n'accède pas à votre position GPS.",
                )
            }
            item {
                PrivacySection(
                    title = "Biométrie et sécurité",
                    content = "Si vous activez la protection, l'authentification est réalisée par Android. L'application ne lit et ne stocke jamais votre empreinte, votre visage, votre code ou votre mot de passe. Elle conserve uniquement le choix d'activer la protection. La base locale n'est pas encore chiffrée.",
                )
            }
            item {
                PrivacySection(
                    title = "Conservation et suppression",
                    content = "Les données restent sur l'appareil jusqu'à leur modification, leur suppression ou la désinstallation de l'application. Une copie gérée par Android peut être restaurée si la sauvegarde système est active. Les futures fonctions de sauvegarde devront rester explicites et contrôlées par l'utilisateur.",
                )
            }
            item {
                PrivacySection(
                    title = "Évolution de cette politique",
                    content = "Cette page devra être mise à jour avant l'ajout d'une synchronisation, d'une sauvegarde distante, de la géolocalisation ou de tout service tiers qui modifierait le traitement des données.",
                )
            }
            item {
                Text(
                    text = "Cette politique décrit le fonctionnement de la version actuelle d'Itinéraire.",
                    modifier = Modifier.padding(bottom = 20.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PrivacySection(
    title: String,
    content: String,
) {
    Card(Modifier.fillMaxWidth()) {
        Text(
            text = title,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = content,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 16.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
