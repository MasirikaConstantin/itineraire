package com.mascode.itineraire.ui.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mascode.itineraire.data.local.entity.PlaceEntity
import com.mascode.itineraire.domain.model.DayEventType
import com.mascode.itineraire.domain.model.JourneyStatus
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
fun TodayScreen(
    viewModel: TodayViewModel,
    onOpenPlaces: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showJourneyDialog by remember { mutableStateOf(false) }

    if (state.isLoading) {
        Column(modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            Text("Aujourd'hui", style = MaterialTheme.typography.headlineMedium)
            Text(
                LocalDate.now().format(
                    DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(Locale.FRENCH),
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        state.errorMessage?.let { message ->
            item {
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(message, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = viewModel::clearError) { Text("OK") }
                    }
                }
            }
        }

        item {
            Text("Actions rapides", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { viewModel.addEvent(DayEventType.WAKE_UP) }) { Text("Réveil") }
                OutlinedButton(onClick = { viewModel.addEvent(DayEventType.LEAVE_HOME) }) { Text("Sortie maison") }
            }
        }

        item {
            Button(
                onClick = {
                    if (state.places.size >= 2) showJourneyDialog = true else onOpenPlaces()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (state.places.size >= 2) "Commencer un trajet" else "Ajouter au moins deux lieux")
            }
        }

        val activeJourneys = state.journeys.filter { it.status == JourneyStatus.IN_PROGRESS }
        if (activeJourneys.isNotEmpty()) {
            item { Text("Trajet en cours", style = MaterialTheme.typography.titleMedium) }
            items(activeJourneys, key = { it.id }) { journey ->
                val source = state.places.firstOrNull { it.id == journey.sourcePlaceId }?.name.orEmpty()
                val destination = state.places.firstOrNull { it.id == journey.destinationPlaceId }?.name.orEmpty()
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("$source → $destination", style = MaterialTheme.typography.titleMedium)
                        Text("Départ : ${formatTime(journey.startedAt)}")
                        Button(onClick = { viewModel.finishJourney(journey.id) }) { Text("Terminer le trajet") }
                    }
                }
            }
        }

        item { Text("Événements", style = MaterialTheme.typography.titleMedium) }
        if (state.events.isEmpty()) {
            item { Text("Aucun événement enregistré aujourd'hui.") }
        } else {
            items(state.events, key = { it.id }) { event ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(formatTime(event.occurredAt), modifier = Modifier.weight(0.25f))
                    Text(eventLabel(event.type), modifier = Modifier.weight(0.75f))
                }
                HorizontalDivider()
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }

    if (showJourneyDialog) {
        NewJourneyDialog(
            places = state.places,
            onDismiss = { showJourneyDialog = false },
            onConfirm = { source, destination ->
                viewModel.startJourney(source, destination)
                showJourneyDialog = false
            },
        )
    }
}

@Composable
private fun NewJourneyDialog(
    places: List<PlaceEntity>,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
) {
    var sourceId by remember { mutableStateOf(places.first().id) }
    var destinationId by remember { mutableStateOf(places.first { it.id != sourceId }.id) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nouveau trajet") },
        text = {
            Column {
                Text("Source", style = MaterialTheme.typography.labelLarge)
                places.forEach { place ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = sourceId == place.id,
                            onClick = {
                                sourceId = place.id
                                if (destinationId == place.id) destinationId = places.first { it.id != place.id }.id
                            },
                        )
                        Text(place.name)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("Destination", style = MaterialTheme.typography.labelLarge)
                places.filter { it.id != sourceId }.forEach { place ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = destinationId == place.id, onClick = { destinationId = place.id })
                        Text(place.name)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(sourceId, destinationId) }) { Text("Démarrer") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}

private fun eventLabel(type: DayEventType) = when (type) {
    DayEventType.WAKE_UP -> "Réveil"
    DayEventType.LEAVE_HOME -> "Sortie de la maison"
    DayEventType.ARRIVAL -> "Arrivée"
    DayEventType.ACTIVITY -> "Activité"
    DayEventType.END_OF_DAY -> "Fin de journée"
}

private fun formatTime(instant: java.time.Instant): String =
    DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault()).format(instant)
