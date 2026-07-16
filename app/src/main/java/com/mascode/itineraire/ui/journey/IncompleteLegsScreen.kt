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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mascode.itineraire.data.local.entity.JourneyLegEntity
import com.mascode.itineraire.data.local.entity.PlaceEntity
import com.mascode.itineraire.domain.model.TransportMode
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

private val dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.FRENCH)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncompleteLegsScreen(
    viewModel: IncompleteLegsViewModel,
    onBack: () -> Unit,
    onEditLeg: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val placesById = remember(state.places) { state.places.associateBy(PlaceEntity::id) }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Données à compléter") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Retour")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    "Complétez ici les tronçons terminés rapidement depuis le widget ou la notification.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            if (state.isLoading) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalArrangement = Arrangement.Center,
                    ) { CircularProgressIndicator() }
                }
            } else if (state.legs.isEmpty()) {
                item { EmptyIncompleteLegs() }
            } else {
                items(state.legs, key = JourneyLegEntity::id) { leg ->
                    IncompleteLegCard(
                        leg = leg,
                        placesById = placesById,
                        onClick = { onEditLeg(leg.id) },
                    )
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun IncompleteLegCard(
    leg: JourneyLegEntity,
    placesById: Map<String, PlaceEntity>,
    onClick: () -> Unit,
) {
    val source = placesById[leg.sourcePlaceId]?.name ?: "Départ non défini"
    val destination = placesById[leg.destinationPlaceId]?.name ?: "Arrivée non définie"
    val endedAt = requireNotNull(leg.endedAt)
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer,
                shape = MaterialTheme.shapes.medium,
            ) {
                Icon(
                    Icons.Outlined.EditNote,
                    contentDescription = null,
                    modifier = Modifier.padding(9.dp).size(24.dp),
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
            Column(
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    "$source → $destination",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "${transportLabel(leg.transportMode)} · ${formatDuration(Duration.between(leg.startedAt, endedAt))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    endedAt.atZone(ZoneId.systemDefault()).format(dateTimeFormatter),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Icon(Icons.Outlined.ChevronRight, contentDescription = "Compléter")
        }
    }
}

@Composable
private fun EmptyIncompleteLegs() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                Icons.Outlined.EditNote,
                contentDescription = null,
                modifier = Modifier.size(38.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text("Tout est à jour", style = MaterialTheme.typography.titleMedium)
            Text(
                "Aucun tronçon ne demande d'informations supplémentaires.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CompleteLegScreen(
    viewModel: IncompleteLegsViewModel,
    legId: String,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val leg = state.legs.firstOrNull { it.id == legId }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Compléter le tronçon") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Retour")
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.isLoading -> Row(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) { CircularProgressIndicator() }
            leg == null -> Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("Ce tronçon a déjà été complété ou n'existe plus.")
                Button(onClick = onBack, modifier = Modifier.padding(top = 16.dp)) { Text("Retour") }
            }
            else -> CompleteLegForm(
                state = state,
                leg = leg,
                onSave = { sourceId, destinationId, mode, start, end, cost, notes ->
                    viewModel.complete(
                        legId = leg.id,
                        sourcePlaceId = sourceId,
                        destinationPlaceId = destinationId,
                        transportMode = mode,
                        startedAt = start,
                        endedAt = end,
                        cost = cost,
                        notes = notes,
                        onSaved = onSaved,
                    )
                },
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CompleteLegForm(
    state: IncompleteLegsUiState,
    leg: JourneyLegEntity,
    onSave: (String?, String?, TransportMode, Instant, Instant, Long, String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val zone = remember { ZoneId.systemDefault() }
    val initialEnd = requireNotNull(leg.endedAt)
    var sourceId by rememberSaveable(leg.id) { mutableStateOf(leg.sourcePlaceId) }
    var destinationId by rememberSaveable(leg.id) { mutableStateOf(leg.destinationPlaceId) }
    var transportMode by rememberSaveable(leg.id) { mutableStateOf(leg.transportMode) }
    var startText by rememberSaveable(leg.id) {
        mutableStateOf(leg.startedAt.atZone(zone).format(dateTimeFormatter))
    }
    var endText by rememberSaveable(leg.id) {
        mutableStateOf(initialEnd.atZone(zone).format(dateTimeFormatter))
    }
    var costText by rememberSaveable(leg.id) { mutableStateOf(leg.cost.takeIf { it > 0 }?.toString().orEmpty()) }
    var notes by rememberSaveable(leg.id) { mutableStateOf(leg.notes.orEmpty()) }
    val start = startText.toInstantOrNull(zone)
    val end = endText.toInstantOrNull(zone)
    val cost = costText.toLongOrNull()
    val chronological = start != null && end != null && !end.isBefore(start)
    val placesDiffer = sourceId == null || sourceId != destinationId
    val canSave = cost != null && chronological && placesDiffer && !state.isSaving
    val costFocusRequester = remember { FocusRequester() }

    LaunchedEffect(leg.id) { costFocusRequester.requestFocus() }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(
                "Vérifiez les informations enregistrées automatiquement, puis ajoutez le prix payé.",
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            OutlinedTextField(
                value = costText,
                onValueChange = { costText = it.filter(Char::isDigit) },
                modifier = Modifier.fillMaxWidth().focusRequester(costFocusRequester),
                label = { Text("Prix payé") },
                suffix = { Text("CDF") },
                leadingIcon = { Icon(Icons.Outlined.Payments, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                supportingText = { Text("Saisissez 0 si le déplacement était gratuit.") },
            )
        }
        item { PlaceSelector("Lieu de départ", state.places, sourceId) { sourceId = it } }
        item { PlaceSelector("Lieu d'arrivée", state.places, destinationId) { destinationId = it } }
        if (!placesDiffer) {
            item {
                Text(
                    "Le départ et l'arrivée doivent être différents.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        item {
            Text("Moyen de transport", style = MaterialTheme.typography.titleMedium)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                maxItemsInEachRow = 3,
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
        item {
            Text("Horaires", style = MaterialTheme.typography.titleMedium)
            Text(
                "Format : jour/mois/année heure:minute",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            OutlinedTextField(
                value = startText,
                onValueChange = { startText = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Départ") },
                leadingIcon = { Icon(Icons.Outlined.Schedule, contentDescription = null) },
                singleLine = true,
                isError = start == null,
            )
        }
        item {
            OutlinedTextField(
                value = endText,
                onValueChange = { endText = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Arrivée") },
                leadingIcon = { Icon(Icons.Outlined.Schedule, contentDescription = null) },
                singleLine = true,
                isError = end == null || !chronological,
                supportingText = {
                    if (!chronological) Text("L'arrivée doit suivre le départ.")
                },
            )
        }
        item {
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Notes facultatives") },
                minLines = 3,
                placeholder = { Text("Embouteillage, attente, détail du paiement…") },
            )
        }
        state.errorMessage?.let { message ->
            item {
                Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
        }
        item {
            Button(
                onClick = { onSave(sourceId, destinationId, transportMode, start!!, end!!, cost!!, notes) },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Enregistrer les informations")
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlaceSelector(
    title: String,
    places: List<PlaceEntity>,
    selectedId: String?,
    onSelected: (String?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            maxItemsInEachRow = 3,
        ) {
            FilterChip(
                selected = selectedId == null,
                onClick = { onSelected(null) },
                label = { Text("Non défini") },
            )
            places.forEach { place ->
                FilterChip(
                    selected = selectedId == place.id,
                    onClick = { onSelected(place.id) },
                    label = { Text(place.name) },
                )
            }
        }
    }
}

private fun String.toInstantOrNull(zone: ZoneId): Instant? = try {
    LocalDateTime.parse(trim(), dateTimeFormatter).atZone(zone).toInstant()
} catch (_: DateTimeParseException) {
    null
}

private fun transportLabel(mode: TransportMode): String = when (mode) {
    TransportMode.WALK -> "À pied"
    TransportMode.TAXI -> "Taxi"
    TransportMode.TAXI_BUS -> "Taxi-bus"
    TransportMode.BUS -> "Bus"
    TransportMode.MOTORCYCLE -> "Moto"
    TransportMode.BICYCLE -> "Vélo"
    TransportMode.PERSONAL_CAR -> "Voiture"
    TransportMode.OTHER -> "Autre"
}

private fun formatDuration(duration: Duration): String {
    val minutes = duration.toMinutes().coerceAtLeast(0)
    val hours = minutes / 60
    val remainingMinutes = minutes % 60
    return if (hours > 0) "${hours} h ${remainingMinutes} min" else "${remainingMinutes} min"
}
