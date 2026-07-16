package com.mascode.itineraire.ui.journey

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mascode.itineraire.data.local.entity.PlaceEntity
import com.mascode.itineraire.ui.today.TodayViewModel

private enum class EndpointSelection {
    SOURCE,
    DESTINATION,
}

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
                windowInsets = WindowInsets(0, 0, 0, 0),
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
                    onStart = { sourceId, destinationId ->
                        viewModel.startJourney(sourceId, destinationId, onStarted)
                    },
                    modifier = Modifier.padding(padding),
                )
            }
        }
    }
}

@Composable
private fun JourneyForm(
    places: List<PlaceEntity>,
    errorMessage: String?,
    onClearError: () -> Unit,
    onStart: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var sourceId by rememberSaveable { mutableStateOf(places.first().id) }
    var destinationId by rememberSaveable { mutableStateOf(places.first { it.id != sourceId }.id) }
    var selection by rememberSaveable { mutableStateOf<EndpointSelection?>(null) }
    val source = places.firstOrNull { it.id == sourceId } ?: places.first()
    val destination = places.firstOrNull { it.id == destinationId }
        ?: places.first { it.id != source.id }
    val availablePlaces = when (selection) {
        EndpointSelection.SOURCE -> places.filter { it.id != destination.id }
        EndpointSelection.DESTINATION -> places.filter { it.id != source.id }
        null -> emptyList()
    }

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
                            selection = null
                        },
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
                onClick = { onStart(source.id, destination.id) },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            ) {
                Text("Démarrer : ${source.name} → ${destination.name}")
            }
        }
    }
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
