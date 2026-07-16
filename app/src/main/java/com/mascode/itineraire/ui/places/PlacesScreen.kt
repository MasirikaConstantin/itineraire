package com.mascode.itineraire.ui.places

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AddLocationAlt
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Church
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mascode.itineraire.data.local.entity.PlaceEntity
import com.mascode.itineraire.domain.model.PlaceCategory

@Composable
fun PlacesScreen(
    viewModel: PlacesViewModel,
    onAddPlace: () -> Unit,
    onEditPlace: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    val filteredPlaces = remember(state.places, query) {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isEmpty()) {
            state.places
        } else {
            state.places.filter { place ->
                place.name.contains(normalizedQuery, ignoreCase = true) ||
                    categoryLabel(place.category).contains(normalizedQuery, ignoreCase = true)
            }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            PlacesHeader()
        }

        item {
            PlacesSummary(
                total = state.places.size,
                located = state.places.count { it.hasPosition },
            )
        }

        item {
            Button(onClick = onAddPlace, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.AddLocationAlt, contentDescription = null)
                Text("  Ajouter un lieu")
            }
        }

        if (state.places.isNotEmpty()) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Rechercher un lieu") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    trailingIcon = if (query.isNotEmpty()) {
                        {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Outlined.Clear, contentDescription = "Effacer la recherche")
                            }
                        }
                    } else {
                        null
                    },
                )
            }
        }

        state.errorMessage?.let { message ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                ) {
                    Text(
                        message,
                        modifier = Modifier.padding(14.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
        }

        when {
            state.places.isEmpty() -> item { EmptyPlaces(searching = false) }
            filteredPlaces.isEmpty() -> item { EmptyPlaces(searching = true) }
            else -> {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Lieux enregistrés",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            filteredPlaces.size.toString(),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                items(filteredPlaces, key = PlaceEntity::id) { place ->
                    PlaceCard(place = place, onClick = { onEditPlace(place.id) })
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun PlacesHeader() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            "Mes lieux",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            "Vos départs, destinations et arrêts habituels.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PlacesSummary(total: Int, located: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = MaterialTheme.shapes.medium,
            ) {
                Icon(
                    Icons.Outlined.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.padding(10.dp).size(26.dp),
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                Text(
                    "$total ${if (total == 1) "lieu enregistré" else "lieux enregistrés"}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "$located avec une position · ${total - located} sans position",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
private fun PlaceCard(place: PlaceEntity, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.medium,
            ) {
                Icon(
                    categoryIcon(place.category),
                    contentDescription = null,
                    modifier = Modifier.padding(10.dp).size(24.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            Column(
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    place.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        categoryLabel(place.category),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (place.hasPosition) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.large,
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Outlined.LocationOn,
                                    contentDescription = null,
                                    modifier = Modifier.size(13.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                                Text(
                                    " Position",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                        }
                    }
                }
            }
            Icon(
                Icons.Outlined.ChevronRight,
                contentDescription = "Modifier le lieu",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EmptyPlaces(searching: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                if (searching) Icons.Outlined.Search else Icons.Outlined.AddLocationAlt,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                if (searching) "Aucun lieu trouvé" else "Aucun lieu enregistré",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                if (searching) "Modifiez votre recherche pour afficher d'autres lieux."
                else "Ajoutez au moins deux lieux pour préparer un trajet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

internal fun categoryIcon(category: PlaceCategory): ImageVector = when (category) {
    PlaceCategory.HOME -> Icons.Outlined.Home
    PlaceCategory.SCHOOL -> Icons.Outlined.School
    PlaceCategory.UNIVERSITY -> Icons.Outlined.Apartment
    PlaceCategory.WORK -> Icons.Outlined.Work
    PlaceCategory.CHURCH -> Icons.Outlined.Church
    PlaceCategory.TRANSPORT_STOP -> Icons.Outlined.DirectionsBus
    PlaceCategory.MARKET -> Icons.Outlined.Storefront
    PlaceCategory.HEALTH -> Icons.Outlined.LocalHospital
    PlaceCategory.ADMINISTRATION -> Icons.Outlined.AccountBalance
    PlaceCategory.RESTAURANT -> Icons.Outlined.Restaurant
    PlaceCategory.LEISURE -> Icons.Outlined.SportsEsports
    PlaceCategory.FAMILY_FRIEND -> Icons.Outlined.Groups
    PlaceCategory.OTHER -> Icons.Outlined.MoreHoriz
}

internal fun categoryLabel(category: PlaceCategory) = when (category) {
    PlaceCategory.HOME -> "Maison"
    PlaceCategory.SCHOOL -> "École"
    PlaceCategory.UNIVERSITY -> "Université"
    PlaceCategory.WORK -> "Travail"
    PlaceCategory.CHURCH -> "Église"
    PlaceCategory.TRANSPORT_STOP -> "Arrêt"
    PlaceCategory.MARKET -> "Marché / commerce"
    PlaceCategory.HEALTH -> "Santé"
    PlaceCategory.ADMINISTRATION -> "Administration"
    PlaceCategory.RESTAURANT -> "Restaurant"
    PlaceCategory.LEISURE -> "Loisirs"
    PlaceCategory.FAMILY_FRIEND -> "Famille / ami"
    PlaceCategory.OTHER -> "Autre"
}

private val PlaceEntity.hasPosition: Boolean
    get() = latitude != null && longitude != null
