package com.mascode.itineraire.ui.journey

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mascode.itineraire.data.local.entity.PlaceEntity
import com.mascode.itineraire.ui.today.TodayViewModel
import com.mascode.itineraire.data.repository.JourneyRepository.PlannedLegInput
import com.mascode.itineraire.domain.model.TransportMode

private enum class EndpointSelection {
    SOURCE,
    DESTINATION,
}

private data class PlannedLegDraft(
    val sourcePlaceId: String,
    val destinationPlaceId: String,
    val transportMode: TransportMode,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartJourneyScreen(
    viewModel: TodayViewModel,
    onBack: () -> Unit,
    onStarted: (String) -> Unit,
    onOpenPlaces: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.clearError()
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Commencer un trajet") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Retour")
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.isLoading -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator()
                }
            }

            state.places.size < 2 -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("Deux lieux sont nécessaires", style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Ajoutez au moins un lieu de départ et un lieu de destination avant de commencer.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(20.dp))
                    Button(onClick = onOpenPlaces) { Text("Gérer les lieux") }
                }
            }

            else -> {
                JourneyForm(
                    places = state.places,
                    errorMessage = state.errorMessage,
                    onClearError = viewModel::clearError,
                    onStart = { sourceId, destinationId, plannedLegs ->
                        viewModel.startJourney(sourceId, destinationId, plannedLegs, onStarted)
                    },
                    modifier = Modifier.padding(padding),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun JourneyForm(
    places: List<PlaceEntity>,
    errorMessage: String?,
    onClearError: () -> Unit,
    onStart: (String, String, List<PlannedLegInput>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var sourceId by rememberSaveable { mutableStateOf(places.first().id) }
    var destinationId by rememberSaveable { mutableStateOf(places.first { it.id != sourceId }.id) }
    var selection by rememberSaveable { mutableStateOf<EndpointSelection?>(null) }
    var plannedLegs by remember { mutableStateOf(emptyList<PlannedLegDraft>()) }
    var showLegEditor by rememberSaveable { mutableStateOf(false) }
    var plannedDestinationId by rememberSaveable { mutableStateOf(destinationId) }
    var plannedTransportMode by rememberSaveable { mutableStateOf(TransportMode.WALK) }
    val source = places.firstOrNull { it.id == sourceId } ?: places.first()
    val destination = places.firstOrNull { it.id == destinationId }
        ?: places.first { it.id != source.id }
    val availablePlaces = when (selection) {
        EndpointSelection.SOURCE -> places.filter { it.id != destination.id }
        EndpointSelection.DESTINATION -> places.filter { it.id != source.id }
        null -> emptyList()
    }
    val nextPlannedSourceId = plannedLegs.lastOrNull()?.destinationPlaceId ?: source.id
    val planIsComplete = plannedLegs.isEmpty() || plannedLegs.last().destinationPlaceId == destination.id

    Column(modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text(
                    "Vérifiez votre itinéraire avant de démarrer. L'heure de départ sera enregistrée automatiquement.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                EndpointCard(
                    label = "Départ",
                    placeName = source.name,
                    icon = Icons.Outlined.LocationOn,
                    isBeingEdited = selection == EndpointSelection.SOURCE,
                    onClick = { selection = EndpointSelection.SOURCE },
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    IconButton(
                        onClick = {
                            val previousSource = sourceId
                            sourceId = destinationId
                            destinationId = previousSource
                            selection = null
                            plannedLegs = emptyList()
                            showLegEditor = false
                        },
                    ) {
                        Icon(Icons.Outlined.SwapVert, contentDescription = "Inverser le trajet")
                    }
                }
            }
            item {
                EndpointCard(
                    label = "Destination finale",
                    placeName = destination.name,
                    icon = Icons.Outlined.Flag,
                    isBeingEdited = selection == EndpointSelection.DESTINATION,
                    onClick = { selection = EndpointSelection.DESTINATION },
                )
            }
            selection?.let { endpoint ->
                item {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (endpoint == EndpointSelection.SOURCE) {
                            "Nouveau lieu de départ"
                        } else {
                            "Nouvelle destination"
                        },
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                items(availablePlaces, key = { it.id }) { place ->
                    PlaceChoice(
                        place = place,
                        selected = when (endpoint) {
                            EndpointSelection.SOURCE -> place.id == source.id
                            EndpointSelection.DESTINATION -> place.id == destination.id
                        },
                        onClick = {
                            onClearError()
                            when (endpoint) {
                                EndpointSelection.SOURCE -> sourceId = place.id
                                EndpointSelection.DESTINATION -> destinationId = place.id
                            }
                            plannedLegs = emptyList()
                            showLegEditor = false
                            selection = null
                        },
                    )
                }
            }
            item {
                Spacer(Modifier.height(6.dp))
                Text("Tronçons prévus", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Facultatif — préparez les étapes que vous commencerez ensuite depuis le trajet en cours.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            items(plannedLegs.indices.toList(), key = { it }) { index ->
                val leg = plannedLegs[index]
                PlannedLegDraftCard(
                    position = index + 1,
                    leg = leg,
                    places = places,
                    onDelete = {
                        plannedLegs = plannedLegs.take(index)
                        showLegEditor = false
                    },
                )
            }
            if (!showLegEditor) {
                item {
                    OutlinedButton(
                        onClick = {
                            plannedDestinationId = if (nextPlannedSourceId != destination.id) {
                                destination.id
                            } else {
                                places.first { it.id != nextPlannedSourceId }.id
                            }
                            showLegEditor = true
                        },
                        enabled = nextPlannedSourceId != destination.id,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (plannedLegs.isEmpty()) "Prévoir un tronçon" else "Ajouter un tronçon prévu")
                    }
                }
            } else {
                item {
                    Text(
                        "Départ : ${places.firstOrNull { it.id == nextPlannedSourceId }?.name.orEmpty()}",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text("Arrivée", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        places.filter { it.id != nextPlannedSourceId }.forEach { place ->
                            FilterChip(
                                selected = plannedDestinationId == place.id,
                                onClick = { plannedDestinationId = place.id },
                                label = { Text(place.name) },
                            )
                        }
                    }
                    Text("Transport", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TransportMode.entries.forEach { mode ->
                            FilterChip(
                                selected = plannedTransportMode == mode,
                                onClick = { plannedTransportMode = mode },
                                label = { Text(transportLabel(mode)) },
                            )
                        }
                    }
                    Button(
                        onClick = {
                            plannedLegs = plannedLegs + PlannedLegDraft(
                                sourcePlaceId = nextPlannedSourceId,
                                destinationPlaceId = plannedDestinationId,
                                transportMode = plannedTransportMode,
                            )
                            showLegEditor = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Ajouter ce tronçon")
                    }
                    TextButton(
                        onClick = { showLegEditor = false },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Annuler")
                    }
                }
            }
            if (!planIsComplete) {
                item {
                    Text(
                        "Le dernier tronçon prévu doit atteindre ${destination.name}.",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            errorMessage?.let { message ->
                item {
                    Text(message, color = MaterialTheme.colorScheme.error)
                }
            }
        }

        Surface(tonalElevation = 3.dp, shadowElevation = 3.dp) {
            Button(
                onClick = {
                    onStart(
                        source.id,
                        destination.id,
                        plannedLegs.map {
                            PlannedLegInput(it.sourcePlaceId, it.destinationPlaceId, it.transportMode)
                        },
                    )
                },
                enabled = planIsComplete && !showLegEditor,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            ) {
                Text("Démarrer : ${source.name} → ${destination.name}")
            }
        }
    }
}

@Composable
private fun PlannedLegDraftCard(
    position: Int,
    leg: PlannedLegDraft,
    places: List<PlaceEntity>,
    onDelete: () -> Unit,
) {
    val placesById = places.associateBy(PlaceEntity::id)
    OutlinedCard(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("$position. ${placesById[leg.sourcePlaceId]?.name} → ${placesById[leg.destinationPlaceId]?.name}")
                Text(transportLabel(leg.transportMode), style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.Delete, contentDescription = "Retirer ce tronçon")
            }
        }
    }
}

private fun transportLabel(mode: TransportMode): String = when (mode) {
    TransportMode.WALK -> "Marche"
    TransportMode.TAXI -> "Taxi"
    TransportMode.TAXI_BUS -> "Taxi-bus"
    TransportMode.BUS -> "Bus"
    TransportMode.MOTORCYCLE -> "Moto"
    TransportMode.BICYCLE -> "Vélo"
    TransportMode.PERSONAL_CAR -> "Voiture personnelle"
    TransportMode.OTHER -> "Autre"
}

@Composable
private fun EndpointCard(
    label: String,
    placeName: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isBeingEdited: Boolean,
    onClick: () -> Unit,
) {
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelMedium)
                Text(placeName, style = MaterialTheme.typography.titleMedium)
            }
            Icon(
                Icons.Outlined.Edit,
                contentDescription = if (isBeingEdited) "Sélection en cours" else "Modifier",
                tint = if (isBeingEdited) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PlaceChoice(
    place: PlaceEntity,
    selected: Boolean,
    onClick: () -> Unit,
) {
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = selected, onClick = null)
            Text(place.name, modifier = Modifier.weight(1f))
        }
    }
}
