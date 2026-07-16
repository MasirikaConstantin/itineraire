package com.mascode.itineraire.ui.statistics

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mascode.itineraire.domain.model.TransportMode
import java.text.NumberFormat
import java.time.Duration
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StatisticsScreen(
    viewModel: StatisticsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedPeriod by remember { mutableStateOf(StatisticsPeriod.MONTH) }
    val transports = remember(state, selectedPeriod) { state.transports(selectedPeriod) }
    val destinations = remember(state, selectedPeriod) { state.destinations(selectedPeriod) }
    val selectedMetric = state.metric(selectedPeriod)

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Statistiques locales") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Retour")
                    }
                },
            )
        },
    ) { padding ->
        if (state.isLoading) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) { CircularProgressIndicator() }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    Text(
                        "Vos habitudes sont calculées uniquement à partir des données enregistrées sur ce téléphone.",
                        modifier = Modifier.padding(top = 6.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                item { ExpensePeriods(state) }
                item {
                    Text("Période analysée", style = MaterialTheme.typography.titleMedium)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        StatisticsPeriod.entries.forEach { period ->
                            FilterChip(
                                selected = selectedPeriod == period,
                                onClick = { selectedPeriod = period },
                                label = { Text(period.label) },
                            )
                        }
                    }
                }
                item { PeriodOverview(selectedMetric, selectedPeriod) }

                if (state.journeys.isEmpty()) {
                    item { EmptyStatistics() }
                } else {
                    item { SectionTitle(Icons.Outlined.DirectionsBus, "Transports les plus utilisés") }
                    if (transports.isEmpty()) {
                        item { EmptySection("Aucun tronçon terminé pendant cette période.") }
                    } else {
                        items(transports, key = { it.mode.name }) { statistic ->
                            TransportStatCard(
                                statistic = statistic,
                                maxCount = transports.maxOf(TransportStatistic::legCount),
                            )
                        }
                    }

                    item { SectionTitle(Icons.Outlined.Place, "Destinations fréquentes") }
                    if (destinations.isEmpty()) {
                        item { EmptySection("Aucune destination enregistrée pendant cette période.") }
                    } else {
                        items(destinations.take(10), key = DestinationStatistic::placeId) { destination ->
                            DestinationStatCard(
                                statistic = destination,
                                maxCount = destinations.maxOf(DestinationStatistic::visitCount),
                            )
                        }
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun ExpensePeriods(state: StatisticsUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Payments, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(
                "Dépenses",
                modifier = Modifier.padding(start = 8.dp),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ExpenseCard("Aujourd'hui", state.today.expense, Modifier.weight(1f))
            ExpenseCard("Semaine", state.week.expense, Modifier.weight(1f))
            ExpenseCard("Mois", state.month.expense, Modifier.weight(1f))
        }
    }
}

@Composable
private fun ExpenseCard(label: String, expense: Long, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(formatCompactCost(expense), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
private fun PeriodOverview(metric: PeriodMetric, period: StatisticsPeriod) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = MaterialTheme.colorScheme.primary, shape = MaterialTheme.shapes.medium) {
                    Icon(
                        Icons.Outlined.BarChart,
                        contentDescription = null,
                        modifier = Modifier.padding(8.dp).size(24.dp),
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
                Column(Modifier.padding(start = 12.dp)) {
                    Text(period.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${metric.journeyCount} trajet${if (metric.journeyCount > 1) "s" else ""} clôturé${if (metric.journeyCount > 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OverviewValue(Icons.Outlined.AccessTime, formatDuration(metric.duration), "Temps en déplacement", Modifier.weight(1f))
                OverviewValue(Icons.Outlined.Payments, formatCost(metric.expense), "Dépenses", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun OverviewValue(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, label: String, modifier: Modifier) {
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.medium) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SectionTitle(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(title, modifier = Modifier.padding(start = 8.dp), style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun TransportStatCard(statistic: TransportStatistic, maxCount: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(transportLabel(statistic.mode), modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                Text(
                    "${statistic.legCount} fois",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            LinearProgressIndicator(
                progress = { statistic.legCount.toFloat() / maxCount.coerceAtLeast(1) },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatDuration(statistic.duration), style = MaterialTheme.typography.bodySmall)
                Text(formatCost(statistic.expense), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun DestinationStatCard(statistic: DestinationStatistic, maxCount: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(statistic.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                Text(
                    "${statistic.visitCount} visite${if (statistic.visitCount > 1) "s" else ""}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            LinearProgressIndicator(
                progress = { statistic.visitCount.toFloat() / maxCount.coerceAtLeast(1) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun EmptyStatistics() = EmptySection("Terminez un premier trajet pour commencer à construire vos statistiques.")

@Composable
private fun EmptySection(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Text(message, modifier = Modifier.padding(18.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
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

private fun formatCost(cost: Long): String =
    "${NumberFormat.getIntegerInstance(Locale.FRENCH).format(cost)} CDF"

private fun formatCompactCost(cost: Long): String = when {
    cost >= 1_000_000 -> String.format(Locale.FRENCH, "%.1f M CDF", cost / 1_000_000.0)
    cost >= 10_000 -> String.format(Locale.FRENCH, "%.0f k CDF", cost / 1_000.0)
    else -> "${NumberFormat.getIntegerInstance(Locale.FRENCH).format(cost)} CDF"
}

private fun formatDuration(duration: Duration): String {
    val minutes = duration.toMinutes().coerceAtLeast(0)
    val hours = minutes / 60
    val remainingMinutes = minutes % 60
    return when {
        hours > 0 -> "${hours} h ${remainingMinutes} min"
        minutes > 0 -> "$minutes min"
        else -> "0 min"
    }
}
