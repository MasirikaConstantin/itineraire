package com.mascode.itineraire.ui.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mascode.itineraire.domain.model.DayEventType
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEventScreen(
    viewModel: TodayViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentTime = LocalTime.now()
    var type by remember { mutableStateOf(DayEventType.ACTIVITY) }
    var placeId by remember { mutableStateOf<String?>(null) }
    var notes by remember { mutableStateOf("") }
    var selectedHour by remember { mutableIntStateOf(currentTime.hour) }
    var selectedMinute by remember { mutableIntStateOf(currentTime.minute) }
    var showTimePicker by remember { mutableStateOf(false) }
    val occurredAt = state.selectedDate
        .atTime(selectedHour, selectedMinute)
        .atZone(ZoneId.systemDefault())
        .toInstant()
    val isFuture = occurredAt.isAfter(Instant.now())

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Ajouter un événement") },
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
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { Spacer(Modifier.height(2.dp)) }
            item {
                Text(
                    state.selectedDate.format(
                        DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(Locale.FRENCH),
                    ),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            item {
                Text("Type d'événement", style = MaterialTheme.typography.titleMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    maxItemsInEachRow = 3,
                ) {
                    DayEventType.entries.forEach { eventType ->
                        FilterChip(
                            selected = type == eventType,
                            onClick = { type = eventType },
                            label = { Text(eventTypeLabel(eventType)) },
                        )
                    }
                }
            }
            item {
                Text("Heure", style = MaterialTheme.typography.titleMedium)
                OutlinedButton(
                    onClick = { showTimePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.Schedule, contentDescription = null)
                    Text("  %02d:%02d".format(selectedHour, selectedMinute))
                }
                if (isFuture) {
                    Text(
                        "L'heure de l'événement ne peut pas être future.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            item {
                Text("Lieu facultatif", style = MaterialTheme.typography.titleMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    maxItemsInEachRow = 3,
                ) {
                    FilterChip(
                        selected = placeId == null,
                        onClick = { placeId = null },
                        label = { Text("Aucun") },
                    )
                    state.places.forEach { place ->
                        FilterChip(
                            selected = placeId == place.id,
                            onClick = { placeId = place.id },
                            label = { Text(place.name) },
                        )
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Note facultative") },
                    placeholder = { Text("Ex. Début des cours, rendez-vous…") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                )
            }
            state.errorMessage?.let { message ->
                item { Text(message, color = MaterialTheme.colorScheme.error) }
            }
            item {
                Button(
                    onClick = {
                        viewModel.addEvent(
                            type = type,
                            occurredAt = occurredAt,
                            placeId = placeId,
                            notes = notes,
                            onSaved = onBack,
                        )
                    },
                    enabled = state.dayId != null && !isFuture,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Enregistrer l'événement")
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    if (showTimePicker) {
        val pickerState = rememberTimePickerState(
            initialHour = selectedHour,
            initialMinute = selectedMinute,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Choisir l'heure") },
            text = { TimeInput(state = pickerState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedHour = pickerState.hour
                        selectedMinute = pickerState.minute
                        showTimePicker = false
                    },
                ) {
                    Text("Valider")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Annuler") }
            },
        )
    }
}

internal fun eventTypeLabel(type: DayEventType): String = when (type) {
    DayEventType.WAKE_UP -> "Réveil"
    DayEventType.LEAVE_HOME -> "Sortie de la maison"
    DayEventType.ARRIVAL -> "Arrivée"
    DayEventType.ACTIVITY -> "Activité"
    DayEventType.END_OF_DAY -> "Fin de journée"
}
