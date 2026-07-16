package com.mascode.itineraire.ui.today

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.NavigateBefore
import androidx.compose.material.icons.automirrored.outlined.NavigateNext
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.automirrored.outlined.EventNote
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import com.mascode.itineraire.domain.model.DayEventType
import com.mascode.itineraire.domain.model.JourneyStatus
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    viewModel: TodayViewModel,
    onOpenPlaces: () -> Unit,
    onOpenJourney: (String) -> Unit,
    onAddEvent: () -> Unit,
    onEditEvent: (String) -> Unit,
    onManageQuickActions: () -> Unit,
    onStartJourney: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showDatePicker by remember { mutableStateOf(false) }
    val today = LocalDate.now()

    Column(modifier.fillMaxSize()) {
        DayNavigationHeader(
            selectedDate = state.selectedDate,
            canNavigateForward = state.selectedDate.isBefore(today),
            onPreviousDay = viewModel::showPreviousDay,
            onNextDay = viewModel::showNextDay,
            onChooseDate = { showDatePicker = true },
        )

        if (state.isLoading) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
            }
        } else {
            DayContent(
                state = state,
                viewModel = viewModel,
                onOpenPlaces = onOpenPlaces,
                onOpenJourney = onOpenJourney,
                onAddEvent = onAddEvent,
                onEditEvent = onEditEvent,
                onManageQuickActions = onManageQuickActions,
                onStartJourney = onStartJourney,
                modifier = Modifier.weight(1f),
            )
        }
    }

    if (showDatePicker) {
        DayPickerDialog(
            selectedDate = state.selectedDate,
            onDismiss = { showDatePicker = false },
            onDateSelected = { date ->
                viewModel.selectDate(date)
                showDatePicker = false
            },
        )
    }
}

@Composable
private fun DayNavigationHeader(
    selectedDate: LocalDate,
    canNavigateForward: Boolean,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onChooseDate: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onPreviousDay) {
                Icon(Icons.AutoMirrored.Outlined.NavigateBefore, contentDescription = "Jour précédent")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    dayTitle(selectedDate),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    selectedDate.format(FULL_DATE_FORMATTER),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            IconButton(onClick = onNextDay, enabled = canNavigateForward) {
                Icon(Icons.AutoMirrored.Outlined.NavigateNext, contentDescription = "Jour suivant")
            }
            IconButton(onClick = onChooseDate) {
                Icon(Icons.Outlined.CalendarMonth, contentDescription = "Choisir une date")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DayContent(
    state: TodayUiState,
    viewModel: TodayViewModel,
    onOpenPlaces: () -> Unit,
    onOpenJourney: (String) -> Unit,
    onAddEvent: () -> Unit,
    onEditEvent: (String) -> Unit,
    onManageQuickActions: () -> Unit,
    onStartJourney: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isToday = state.selectedDate == LocalDate.now()

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Spacer(Modifier.height(4.dp)) }

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

        if (isToday) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Actions rapides",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            IconButton(onClick = onManageQuickActions) {
                                Icon(Icons.Outlined.Edit, contentDescription = "Gérer les actions rapides")
                            }
                        }
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            FilledTonalButton(onClick = { viewModel.addEvent(DayEventType.WAKE_UP) }) {
                                Icon(Icons.Outlined.Bedtime, contentDescription = null)
                                Text("  Réveil")
                            }
                            FilledTonalButton(onClick = { viewModel.addEvent(DayEventType.LEAVE_HOME) }) {
                                Icon(Icons.AutoMirrored.Outlined.DirectionsWalk, contentDescription = null)
                                Text("  Sortie maison")
                            }
                            state.quickActions.forEach { action ->
                                FilledTonalButton(onClick = { viewModel.runQuickAction(action) }) {
                                    Text(action.label)
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onAddEvent,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.AutoMirrored.Outlined.EventNote, contentDescription = null)
                    Text("  Événement")
                }
                if (isToday) {
                    Button(
                        onClick = {
                            if (state.places.size >= 2) onStartJourney() else onOpenPlaces()
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            if (state.places.size >= 2) Icons.Outlined.Route else Icons.Outlined.Place,
                            contentDescription = null,
                        )
                        Text(if (state.places.size >= 2) "  Trajet" else "  Ajouter lieux")
                    }
                }
            }
        }

        if (isToday && state.places.size < 2) {
            item {
                Text(
                    "Ajoutez au moins deux lieux pour commencer un trajet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        val activeJourneys = state.journeys.filter { it.status == JourneyStatus.IN_PROGRESS }
        if (activeJourneys.isNotEmpty()) {
            item { SectionHeader("Trajet en cours") }
            items(activeJourneys, key = { it.id }) { journey ->
                val source = state.places.firstOrNull { it.id == journey.sourcePlaceId }?.name.orEmpty()
                val destination = state.places.firstOrNull { it.id == journey.destinationPlaceId }?.name.orEmpty()
                Card(
                    onClick = { onOpenJourney(journey.id) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.Route, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text("$source → $destination", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Départ à ${formatTime(journey.startedAt)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                        Icon(Icons.Outlined.ChevronRight, contentDescription = "Ouvrir le trajet")
                    }
                }
            }
        }

        item { SectionHeader("Événements", state.events.size.toString()) }
        if (state.events.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            Icons.Outlined.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            if (isToday) {
                                "Votre journée est encore vide"
                            } else {
                                "Aucun événement à cette date"
                            },
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            if (isToday) "Ajoutez un événement ou utilisez une action rapide." else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        } else {
            items(state.events, key = { it.id }) { event ->
                Card(
                    onClick = { onEditEvent(event.id) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = MaterialTheme.shapes.medium,
                        ) {
                            Text(
                                formatTime(event.occurredAt),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(eventTypeLabel(event.type), style = MaterialTheme.typography.titleMedium)
                            event.placeId?.let { placeId ->
                                state.places.firstOrNull { it.id == placeId }?.let { place ->
                                    Text(place.name, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            event.notes?.let { notes ->
                                Text(notes, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun SectionHeader(title: String, count: String? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
        count?.let {
            Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.extraLarge) {
                Text(it, modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayPickerDialog(
    selectedDate: LocalDate,
    onDismiss: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
) {
    val todayUtcMillis = LocalDate.now().toUtcMillis()
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate.toUtcMillis(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis <= todayUtcMillis
        },
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    pickerState.selectedDateMillis?.let { onDateSelected(it.toLocalDateUtc()) }
                },
                enabled = pickerState.selectedDateMillis != null,
            ) {
                Text("Afficher")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    ) {
        DatePicker(
            state = pickerState,
            title = { Text("Choisir une journée", modifier = Modifier.padding(start = 24.dp, top = 16.dp)) },
        )
    }
}

private fun formatTime(instant: java.time.Instant): String =
    DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault()).format(instant)

private val FULL_DATE_FORMATTER =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(Locale.FRENCH)
private val DAY_NAME_FORMATTER = DateTimeFormatter.ofPattern("EEEE", Locale.FRENCH)

private fun dayTitle(date: LocalDate): String = when (date) {
    LocalDate.now() -> "Aujourd'hui"
    LocalDate.now().minusDays(1) -> "Hier"
    else -> date
        .format(DAY_NAME_FORMATTER)
        .replaceFirstChar { it.titlecase(Locale.FRENCH) }
}

private fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toLocalDateUtc(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
