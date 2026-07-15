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
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mascode.itineraire.data.local.entity.PlaceEntity
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
    onManageQuickActions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showJourneyDialog by remember { mutableStateOf(false) }
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
                onManageQuickActions = onManageQuickActions,
                onStartJourney = { showJourneyDialog = true },
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

    if (showJourneyDialog) {
        NewJourneyDialog(
            places = state.places,
            onDismiss = { showJourneyDialog = false },
            onConfirm = { source, destination ->
                viewModel.startJourney(source, destination, onOpenJourney)
                showJourneyDialog = false
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
    Surface(color = MaterialTheme.colorScheme.background) {
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
                Text(dayTitle(selectedDate), style = MaterialTheme.typography.headlineMedium)
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
                Text("Actions rapides", style = MaterialTheme.typography.titleMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    OutlinedButton(onClick = { viewModel.addEvent(DayEventType.WAKE_UP) }) { Text("Réveil") }
                    OutlinedButton(onClick = { viewModel.addEvent(DayEventType.LEAVE_HOME) }) { Text("Sortie maison") }
                    state.quickActions.forEach { action ->
                        OutlinedButton(onClick = { viewModel.runQuickAction(action) }) {
                            Text(action.label)
                        }
                    }
                }
                TextButton(onClick = onManageQuickActions) { Text("Gérer les actions rapides") }
            }
        }

        item {
            OutlinedButton(
                onClick = onAddEvent,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Ajouter un événement")
            }
        }

        if (isToday) {
            item {
                Button(
                    onClick = {
                        if (state.places.size >= 2) onStartJourney() else onOpenPlaces()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (state.places.size >= 2) "Commencer un trajet" else "Ajouter au moins deux lieux")
                }
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
                        Button(onClick = { onOpenJourney(journey.id) }) { Text("Ouvrir le trajet") }
                    }
                }
            }
        }

        item { Text("Événements", style = MaterialTheme.typography.titleMedium) }
        if (state.events.isEmpty()) {
            item {
                Text(
                    if (isToday) {
                        "Aucun événement enregistré aujourd'hui."
                    } else {
                        "Aucun événement enregistré à cette date."
                    },
                )
            }
        } else {
            items(state.events, key = { it.id }) { event ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(formatTime(event.occurredAt), modifier = Modifier.weight(0.25f))
                    Column(modifier = Modifier.weight(0.75f)) {
                        Text(eventTypeLabel(event.type))
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
                HorizontalDivider()
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
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
