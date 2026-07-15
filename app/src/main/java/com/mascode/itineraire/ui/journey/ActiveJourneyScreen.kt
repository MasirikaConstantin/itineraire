package com.mascode.itineraire.ui.journey

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mascode.itineraire.data.local.entity.JourneyLegEntity
import com.mascode.itineraire.data.local.entity.JourneyObservationEntity
import com.mascode.itineraire.data.local.entity.PlaceEntity
import com.mascode.itineraire.domain.model.JourneyStatus
import com.mascode.itineraire.domain.model.ObservationType
import com.mascode.itineraire.domain.model.TransportMode
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveJourneyScreen(
    viewModel: ActiveJourneyViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val journey = state.journey
    var showStartLegDialog by remember { mutableStateOf(false) }
    var legToFinish by remember { mutableStateOf<JourneyLegEntity?>(null) }
    var showObservationDialog by remember { mutableStateOf(false) }
    var showFinishConfirmation by remember { mutableStateOf(false) }
    var showCancelConfirmation by remember { mutableStateOf(false) }
    val currentTime by produceState(initialValue = Instant.now(), journey?.status) {
        while (journey?.status == JourneyStatus.IN_PROGRESS) {
            value = Instant.now()
            delay(1_000)
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(if (journey?.status == JourneyStatus.IN_PROGRESS) "Trajet en cours" else "Résumé du trajet")
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Retour")
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        when {
            state.isLoading -> Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
            }

            journey == null -> Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Ce trajet est introuvable.")
                Spacer(Modifier.height(12.dp))
                Button(onClick = onBack) { Text("Retour") }
            }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { Spacer(Modifier.height(4.dp)) }
                item {
                    JourneySummaryCard(
                        state = state,
                        now = currentTime,
                    )
                }

                state.errorMessage?.let { message ->
                    item {
                        Card(Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = message,
                                    modifier = Modifier.weight(1f),
                                    color = MaterialTheme.colorScheme.error,
                                )
                                TextButton(onClick = viewModel::clearError) { Text("OK") }
                            }
                        }
                    }
                }

                if (journey.status == JourneyStatus.IN_PROGRESS) {
                    state.activeLeg?.let { activeLeg ->
                        item {
                            ActiveLegCard(
                                leg = activeLeg,
                                places = state.places,
                                now = currentTime,
                                onFinish = { legToFinish = activeLeg },
                            )
                        }
                    } ?: item {
                        Button(
                            onClick = { showStartLegDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(if (state.legs.isEmpty()) "Démarrer le premier tronçon" else "Ajouter un tronçon")
                        }
                    }

                    item {
                        OutlinedButton(
                            onClick = { showObservationDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Ajouter une observation")
                        }
                    }
                }

                if (state.legs.isNotEmpty()) {
                    item { Text("Tronçons", style = MaterialTheme.typography.titleLarge) }
                    items(state.legs, key = { it.id }) { leg ->
                        LegCard(leg = leg, places = state.places, now = currentTime)
                    }
                }

                if (state.observations.isNotEmpty()) {
                    item { Text("Observations", style = MaterialTheme.typography.titleLarge) }
                    items(state.observations, key = { it.id }) { observation ->
                        ObservationRow(observation)
                    }
                }

                if (journey.status == JourneyStatus.IN_PROGRESS) {
                    item {
                        HorizontalDivider()
                        Spacer(Modifier.height(4.dp))
                        Button(
                            onClick = { showFinishConfirmation = true },
                            enabled = state.activeLeg == null && state.hasReachedFinalDestination,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Terminer le trajet")
                        }
                        val finishHint = when {
                            state.activeLeg != null -> "Terminez le tronçon actif avant de clôturer le trajet."
                            state.legs.isEmpty() -> "Ajoutez au moins un tronçon pour terminer le trajet."
                            !state.hasReachedFinalDestination -> "Le dernier tronçon doit atteindre la destination finale."
                            else -> null
                        }
                        finishHint?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        TextButton(
                            onClick = { showCancelConfirmation = true },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Annuler le trajet", color = MaterialTheme.colorScheme.error)
                        }
                    }
                } else {
                    item {
                        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                            Text("Retour à l'accueil")
                        }
                    }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }

    if (showStartLegDialog && journey != null) {
        StartLegDialog(
            places = state.places,
            sourcePlaceId = state.nextSourcePlaceId,
            preferredDestinationId = journey.destinationPlaceId,
            onDismiss = { showStartLegDialog = false },
            onConfirm = { destinationId, transportMode ->
                viewModel.startLeg(state.nextSourcePlaceId, destinationId, transportMode)
                showStartLegDialog = false
            },
        )
    }

    legToFinish?.let { leg ->
        FinishLegDialog(
            onDismiss = { legToFinish = null },
            onConfirm = { cost, notes ->
                viewModel.finishLeg(leg.id, cost, notes)
                legToFinish = null
            },
        )
    }

    if (showObservationDialog) {
        ObservationDialog(
            onDismiss = { showObservationDialog = false },
            onConfirm = { type, notes ->
                viewModel.addObservation(type, notes)
                showObservationDialog = false
            },
        )
    }

    if (showFinishConfirmation) {
        ConfirmationDialog(
            title = "Terminer le trajet ?",
            message = "La durée et le coût total seront conservés dans l'historique.",
            confirmLabel = "Terminer",
            onDismiss = { showFinishConfirmation = false },
            onConfirm = {
                viewModel.finishJourney()
                showFinishConfirmation = false
            },
        )
    }

    if (showCancelConfirmation) {
        ConfirmationDialog(
            title = "Annuler le trajet ?",
            message = "Les informations déjà enregistrées resteront visibles dans l'historique avec le statut annulé.",
            confirmLabel = "Annuler le trajet",
            destructive = true,
            onDismiss = { showCancelConfirmation = false },
            onConfirm = {
                viewModel.cancelJourney()
                showCancelConfirmation = false
            },
        )
    }
}

@Composable
private fun JourneySummaryCard(state: ActiveJourneyUiState, now: Instant) {
    val journey = state.journey ?: return
    val places = state.places.associateBy(PlaceEntity::id)
    val end = journey.endedAt ?: now

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "${places[journey.sourcePlaceId]?.name.orUnknown()} → ${places[journey.destinationPlaceId]?.name.orUnknown()}",
                style = MaterialTheme.typography.titleLarge,
            )
            Text("Départ : ${formatTime(journey.startedAt)}")
            Text("Durée totale : ${formatDuration(Duration.between(journey.startedAt, end))}")
            Text("Coût total : ${formatCost(state.totalCost)}", style = MaterialTheme.typography.titleMedium)
            Text(statusLabel(journey.status), color = statusColor(journey.status))
        }
    }
}

@Composable
private fun ActiveLegCard(
    leg: JourneyLegEntity,
    places: List<PlaceEntity>,
    now: Instant,
    onFinish: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Tronçon en cours", style = MaterialTheme.typography.titleLarge)
            Text(legRoute(leg, places), style = MaterialTheme.typography.titleMedium)
            Text(transportLabel(leg.transportMode))
            Text("Depuis ${formatTime(leg.startedAt)} · ${formatDuration(Duration.between(leg.startedAt, now))}")
            Button(onClick = onFinish, modifier = Modifier.fillMaxWidth()) { Text("Terminer ce tronçon") }
        }
    }
}

@Composable
private fun LegCard(leg: JourneyLegEntity, places: List<PlaceEntity>, now: Instant) {
    val end = leg.endedAt ?: now
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("${leg.position + 1}. ${legRoute(leg, places)}", style = MaterialTheme.typography.titleMedium)
            Text("${transportLabel(leg.transportMode)} · ${formatDuration(Duration.between(leg.startedAt, end))}")
            Text("${formatTime(leg.startedAt)} → ${leg.endedAt?.let(::formatTime) ?: "en cours"}")
            if (leg.endedAt != null) Text(formatCost(leg.cost), style = MaterialTheme.typography.titleSmall)
            leg.notes?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
private fun ObservationRow(observation: JourneyObservationEntity) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Text(observationLabel(observation.type), modifier = Modifier.weight(1f))
            Text(formatTime(observation.occurredAt), style = MaterialTheme.typography.bodySmall)
        }
        observation.notes?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        HorizontalDivider(Modifier.padding(top = 6.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StartLegDialog(
    places: List<PlaceEntity>,
    sourcePlaceId: String?,
    preferredDestinationId: String,
    onDismiss: () -> Unit,
    onConfirm: (String, TransportMode) -> Unit,
) {
    val destinations = places.filter { it.id != sourcePlaceId }
    var destinationId by remember(destinations, preferredDestinationId) {
        mutableStateOf(
            destinations.firstOrNull { it.id == preferredDestinationId }?.id
                ?: destinations.firstOrNull()?.id,
        )
    }
    var transportMode by remember { mutableStateOf(TransportMode.WALK) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nouveau tronçon") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Départ", style = MaterialTheme.typography.labelLarge)
                Text(places.firstOrNull { it.id == sourcePlaceId }?.name.orUnknown())
                Text("Arrivée", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    destinations.forEach { place ->
                        FilterChip(
                            selected = destinationId == place.id,
                            onClick = { destinationId = place.id },
                            label = { Text(place.name) },
                        )
                    }
                }
                Text("Transport", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    TransportMode.entries.forEach { mode ->
                        FilterChip(
                            selected = transportMode == mode,
                            onClick = { transportMode = mode },
                            label = { Text(transportLabel(mode)) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { destinationId?.let { onConfirm(it, transportMode) } },
                enabled = destinationId != null,
            ) {
                Text("Démarrer")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}

@Composable
private fun FinishLegDialog(onDismiss: () -> Unit, onConfirm: (Long, String?) -> Unit) {
    var costText by remember { mutableStateOf("0") }
    var notes by remember { mutableStateOf("") }
    val cost = costText.toLongOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Terminer le tronçon") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = costText,
                    onValueChange = { value -> costText = value.filter(Char::isDigit) },
                    label = { Text("Prix payé") },
                    suffix = { Text("CDF") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Note facultative") },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { cost?.let { onConfirm(it, notes) } }, enabled = cost != null) {
                Text("Terminer")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ObservationDialog(onDismiss: () -> Unit, onConfirm: (ObservationType, String?) -> Unit) {
    var type by remember { mutableStateOf(ObservationType.TRAFFIC) }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajouter une observation") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    ObservationType.entries.forEach { item ->
                        FilterChip(
                            selected = type == item,
                            onClick = { type = item },
                            label = { Text(observationLabel(item)) },
                        )
                    }
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Détails facultatifs") },
                )
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(type, notes) }) { Text("Ajouter") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}

@Composable
private fun ConfirmationDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    destructive: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    confirmLabel,
                    color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Retour") } },
    )
}

private fun legRoute(leg: JourneyLegEntity, places: List<PlaceEntity>): String {
    val placesById = places.associateBy(PlaceEntity::id)
    return "${placesById[leg.sourcePlaceId]?.name.orUnknown()} → ${placesById[leg.destinationPlaceId]?.name.orUnknown()}"
}

private fun String?.orUnknown(): String = this ?: "Lieu inconnu"

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

private fun observationLabel(type: ObservationType): String = when (type) {
    ObservationType.TRAFFIC -> "Embouteillage"
    ObservationType.WAITING -> "Attente"
    ObservationType.BREAKDOWN -> "Panne"
    ObservationType.WEATHER -> "Météo"
    ObservationType.OTHER -> "Autre"
}

private fun statusLabel(status: JourneyStatus): String = when (status) {
    JourneyStatus.IN_PROGRESS -> "En cours"
    JourneyStatus.COMPLETED -> "Terminé"
    JourneyStatus.CANCELLED -> "Annulé"
}

@Composable
private fun statusColor(status: JourneyStatus) = when (status) {
    JourneyStatus.IN_PROGRESS -> MaterialTheme.colorScheme.primary
    JourneyStatus.COMPLETED -> MaterialTheme.colorScheme.tertiary
    JourneyStatus.CANCELLED -> MaterialTheme.colorScheme.error
}

private fun formatTime(instant: Instant): String =
    DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault()).format(instant)

private fun formatCost(cost: Long): String =
    "${java.text.NumberFormat.getIntegerInstance(java.util.Locale.FRENCH).format(cost)} CDF"

private fun formatDuration(duration: Duration): String {
    val seconds = duration.seconds.coerceAtLeast(0)
    val hours = seconds / 3_600
    val minutes = (seconds % 3_600) / 60
    val remainingSeconds = seconds % 60
    return when {
        hours > 0 -> "${hours}h ${minutes}min"
        minutes > 0 -> "${minutes}min ${remainingSeconds}s"
        else -> "${remainingSeconds}s"
    }
}
