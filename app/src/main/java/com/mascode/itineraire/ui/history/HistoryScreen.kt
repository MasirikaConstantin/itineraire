package com.mascode.itineraire.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mascode.itineraire.data.local.entity.JourneyEntity
import com.mascode.itineraire.domain.model.JourneyStatus
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class HistoryFilter(val label: String) {
    ALL("Tous"),
    COMPLETED("Terminés"),
    IN_PROGRESS("En cours"),
    CANCELLED("Annulés"),
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onOpenJourney: (String) -> Unit,
    onOpenIncompleteLegs: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedFilter by remember { mutableStateOf(HistoryFilter.ALL) }
    val filteredJourneys = remember(state.journeys, selectedFilter) {
        state.journeys.filter { journey -> selectedFilter.matches(journey.status) }
    }
    val journeysByDay = remember(filteredJourneys) {
        filteredJourneys.groupBy { it.startedAt.atZone(ZoneId.systemDefault()).toLocalDate() }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            HistoryHeader()
        }

        if (state.journeys.isNotEmpty()) {
            item {
                HistorySummary(state.journeys)
            }
            if (state.incompleteLegs.isNotEmpty()) {
                item {
                    IncompleteDataCard(
                        count = state.incompleteLegs.size,
                        onClick = onOpenIncompleteLegs,
                    )
                }
            }
            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    HistoryFilter.entries.forEach { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter.label) },
                        )
                    }
                }
            }
        }

        if (filteredJourneys.isEmpty()) {
            item {
                EmptyHistory(filtered = state.journeys.isNotEmpty())
            }
        } else {
            journeysByDay.forEach { (date, journeys) ->
                item(key = "day-$date") {
                    DayHeader(date = date, count = journeys.size)
                }
                items(journeys, key = JourneyEntity::id) { journey ->
                    val source = state.placesById[journey.sourcePlaceId]?.name ?: "Lieu inconnu"
                    val destination = state.placesById[journey.destinationPlaceId]?.name ?: "Lieu inconnu"
                    JourneyHistoryCard(
                        journey = journey,
                        source = source,
                        destination = destination,
                        onClick = { onOpenJourney(journey.id) },
                    )
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun IncompleteDataCard(count: Int, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.EditNote,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(
                    "$count tronçon${if (count > 1) "s" else ""} à compléter",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                Text(
                    "Ajoutez le prix et vérifiez les informations enregistrées.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
            Icon(
                Icons.Outlined.ChevronRight,
                contentDescription = "Ouvrir",
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
    }
}

@Composable
private fun HistoryHeader() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            "Historique",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            "Consultez tous vos déplacements enregistrés.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HistorySummary(journeys: List<JourneyEntity>) {
    val completed = journeys.count { it.status == JourneyStatus.COMPLETED }
    val active = journeys.count { it.status == JourneyStatus.IN_PROGRESS }
    val totalDuration = journeys.fold(Duration.ZERO) { total, journey ->
        if (journey.status == JourneyStatus.COMPLETED && journey.endedAt != null) {
            total.plus(Duration.between(journey.startedAt, journey.endedAt))
        } else {
            total
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Icon(
                        Icons.Outlined.Timeline,
                        contentDescription = null,
                        modifier = Modifier.padding(8.dp).size(24.dp),
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
                Column(Modifier.padding(start = 12.dp)) {
                    Text(
                        "Vue d'ensemble",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        if (active > 0) "$active trajet en cours" else "Vos déplacements enregistrés",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                SummaryMetric(journeys.size.toString(), "Trajets")
                SummaryMetric(completed.toString(), "Terminés")
                SummaryMetric(formatCompactDuration(totalDuration), "Temps total")
            }
        }
    }
}

@Composable
private fun SummaryMetric(value: String, label: String) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
        )
    }
}

@Composable
private fun DayHeader(date: LocalDate, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            dayLabel(date),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            shape = MaterialTheme.shapes.large,
        ) {
            Text(
                count.toString(),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun JourneyHistoryCard(
    journey: JourneyEntity,
    source: String,
    destination: String,
    onClick: () -> Unit,
) {
    val presentation = statusPresentation(journey.status)
    val duration = journey.endedAt?.let { Duration.between(journey.startedAt, it) }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = presentation.containerColor(),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Icon(
                        presentation.icon,
                        contentDescription = null,
                        modifier = Modifier.padding(8.dp).size(22.dp),
                        tint = presentation.contentColor(),
                    )
                }
                Column(
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        "$source → $destination",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                    )
                    Text(
                        "Départ à ${formatTime(journey.startedAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    Icons.Outlined.ChevronRight,
                    contentDescription = "Ouvrir le trajet",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusBadge(presentation)
                duration?.let {
                    InfoBadge(
                        icon = Icons.Outlined.Schedule,
                        label = formatDuration(it),
                    )
                }
                journey.endedAt?.let {
                    Text(
                        "Arrivée ${formatTime(it)}",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(presentation: StatusPresentation) {
    Surface(
        color = presentation.containerColor(),
        shape = MaterialTheme.shapes.large,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Icon(
                presentation.icon,
                contentDescription = null,
                modifier = Modifier.size(15.dp),
                tint = presentation.contentColor(),
            )
            Text(
                presentation.label,
                style = MaterialTheme.typography.labelMedium,
                color = presentation.contentColor(),
            )
        }
    }
}

@Composable
private fun InfoBadge(icon: ImageVector, label: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = MaterialTheme.shapes.large,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(15.dp))
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun EmptyHistory(filtered: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                Icon(
                    if (filtered) Icons.Outlined.Route else Icons.Outlined.History,
                    contentDescription = null,
                    modifier = Modifier.padding(14.dp).size(32.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            Text(
                if (filtered) "Aucun trajet pour ce filtre" else "Aucun trajet enregistré",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                if (filtered) {
                    "Choisissez un autre état pour retrouver vos déplacements."
                } else {
                    "Vos prochains déplacements apparaîtront ici."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private data class StatusPresentation(
    val label: String,
    val icon: ImageVector,
    val status: JourneyStatus,
)

@Composable
private fun StatusPresentation.containerColor(): Color = when (status) {
    JourneyStatus.IN_PROGRESS -> MaterialTheme.colorScheme.primaryContainer
    JourneyStatus.COMPLETED -> MaterialTheme.colorScheme.secondaryContainer
    JourneyStatus.CANCELLED -> MaterialTheme.colorScheme.errorContainer
}

@Composable
private fun StatusPresentation.contentColor(): Color = when (status) {
    JourneyStatus.IN_PROGRESS -> MaterialTheme.colorScheme.onPrimaryContainer
    JourneyStatus.COMPLETED -> MaterialTheme.colorScheme.onSecondaryContainer
    JourneyStatus.CANCELLED -> MaterialTheme.colorScheme.onErrorContainer
}

private fun statusPresentation(status: JourneyStatus): StatusPresentation = when (status) {
    JourneyStatus.IN_PROGRESS -> StatusPresentation("En cours", Icons.Outlined.Route, status)
    JourneyStatus.COMPLETED -> StatusPresentation("Terminé", Icons.Outlined.CheckCircle, status)
    JourneyStatus.CANCELLED -> StatusPresentation("Annulé", Icons.Outlined.Cancel, status)
}

private fun HistoryFilter.matches(status: JourneyStatus): Boolean = when (this) {
    HistoryFilter.ALL -> true
    HistoryFilter.COMPLETED -> status == JourneyStatus.COMPLETED
    HistoryFilter.IN_PROGRESS -> status == JourneyStatus.IN_PROGRESS
    HistoryFilter.CANCELLED -> status == JourneyStatus.CANCELLED
}

private fun dayLabel(date: LocalDate): String = when (date) {
    LocalDate.now() -> "Aujourd'hui"
    LocalDate.now().minusDays(1) -> "Hier"
    else -> date.format(DAY_FORMATTER).replaceFirstChar { it.titlecase(Locale.FRENCH) }
}

private fun formatTime(instant: Instant): String = TIME_FORMATTER.format(instant)

private fun formatDuration(duration: Duration): String {
    val minutes = duration.toMinutes().coerceAtLeast(0)
    val hours = minutes / 60
    return if (hours > 0) "${hours} h ${minutes % 60} min" else "${minutes} min"
}

private fun formatCompactDuration(duration: Duration): String {
    val minutes = duration.toMinutes().coerceAtLeast(0)
    return when {
        minutes >= 60 -> "${minutes / 60}h ${minutes % 60}m"
        else -> "${minutes} min"
    }
}

private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
private val DAY_FORMATTER = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH)
