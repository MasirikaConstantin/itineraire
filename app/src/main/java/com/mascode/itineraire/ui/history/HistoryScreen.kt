package com.mascode.itineraire.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mascode.itineraire.domain.model.JourneyStatus
import java.time.Duration
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onOpenJourney: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            Text("Historique", style = MaterialTheme.typography.headlineMedium)
        }
        if (state.journeys.isEmpty()) {
            item { Text("Aucun trajet enregistré.") }
        } else {
            items(state.journeys, key = { it.id }) { journey ->
                val source = state.placesById[journey.sourcePlaceId]?.name ?: "Lieu inconnu"
                val destination = state.placesById[journey.destinationPlaceId]?.name ?: "Lieu inconnu"
                Card(onClick = { onOpenJourney(journey.id) }, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("$source → $destination", style = MaterialTheme.typography.titleMedium)
                        Text(formatDateTime(journey.startedAt))
                        Text(
                            when (journey.status) {
                                JourneyStatus.IN_PROGRESS -> "En cours"
                                JourneyStatus.CANCELLED -> "Annulé"
                                JourneyStatus.COMPLETED -> journey.endedAt?.let {
                                    "Durée : ${formatDuration(Duration.between(journey.startedAt, it))}"
                                } ?: "Terminé"
                            },
                        )
                    }
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

private fun formatDateTime(instant: java.time.Instant): String =
    DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm").withZone(ZoneId.systemDefault()).format(instant)

private fun formatDuration(duration: Duration): String {
    val hours = duration.toHours()
    val minutes = duration.toMinutes() % 60
    return if (hours > 0) "${hours} h ${minutes} min" else "${minutes} min"
}
