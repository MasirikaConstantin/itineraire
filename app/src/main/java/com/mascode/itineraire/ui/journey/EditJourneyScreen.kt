package com.mascode.itineraire.ui.journey

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mascode.itineraire.data.local.entity.PlaceEntity
import com.mascode.itineraire.domain.model.JourneyStatus
import java.time.Instant
import java.time.ZoneId

private enum class JourneyTimeTarget { START, END }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditJourneyScreen(
    viewModel: ActiveJourneyViewModel,
    onBack: () -> Unit,
    onDeleted: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val journey = state.journey
    var confirmDelete by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Modifier le trajet") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Retour")
                    }
                },
            )
        },
    ) { padding ->
        if (journey == null || state.isLoading) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) { CircularProgressIndicator() }
        } else if (journey.status == JourneyStatus.IN_PROGRESS || journey.endedAt == null) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("Seuls les trajets terminés ou annulés peuvent être modifiés ici.")
                Button(onClick = onBack, modifier = Modifier.padding(top = 16.dp)) { Text("Retour") }
            }
        } else {
            var sourceId by rememberSaveable(journey.id) { mutableStateOf(journey.sourcePlaceId) }
            var destinationId by rememberSaveable(journey.id) { mutableStateOf(journey.destinationPlaceId) }
            var startMillis by rememberSaveable(journey.id) { mutableLongStateOf(journey.startedAt.toEpochMilli()) }
            var endMillis by rememberSaveable(journey.id) { mutableLongStateOf(journey.endedAt.toEpochMilli()) }
            var notes by rememberSaveable(journey.id) { mutableStateOf(journey.notes.orEmpty()) }
            var target by remember { mutableStateOf<JourneyTimeTarget?>(null) }
            val start = Instant.ofEpochMilli(startMillis)
            val end = Instant.ofEpochMilli(endMillis)
            val valid = sourceId != destinationId && !end.isBefore(start)
            val zone = remember { ZoneId.systemDefault() }

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item { JourneyPlaceSelector("Source", state.places, sourceId) { sourceId = it } }
                item { JourneyPlaceSelector("Destination", state.places, destinationId) { destinationId = it } }
                if (sourceId == destinationId) {
                    item { Text("La source et la destination doivent être différentes.", color = MaterialTheme.colorScheme.error) }
                }
                item {
                    DateTimeSelector("Départ", start, zone) { target = JourneyTimeTarget.START }
                }
                item {
                    DateTimeSelector("Arrivée", end, zone) { target = JourneyTimeTarget.END }
                }
                if (end.isBefore(start)) {
                    item { Text("L'arrivée doit suivre le départ.", color = MaterialTheme.colorScheme.error) }
                }
                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes facultatives") },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                state.errorMessage?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
                item {
                    Button(
                        onClick = {
                            viewModel.updateFinishedJourney(sourceId, destinationId, start, end, notes, onBack)
                        },
                        enabled = valid,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                    ) { Text("Enregistrer les modifications") }
                }
                item {
                    TextButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Supprimer le trajet", color = MaterialTheme.colorScheme.error)
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }

            target?.let { selectedTarget ->
                DateTimePickerDialog(
                    initialInstant = if (selectedTarget == JourneyTimeTarget.START) start else end,
                    zone = zone,
                    onDismiss = { target = null },
                    onConfirm = {
                        if (selectedTarget == JourneyTimeTarget.START) startMillis = it.toEpochMilli()
                        else endMillis = it.toEpochMilli()
                        target = null
                    },
                )
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Supprimer ce trajet ?") },
            text = { Text("Le trajet, ses tronçons et ses observations seront définitivement supprimés.") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteFinishedJourney(onDeleted) }) {
                    Text("Supprimer", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Annuler") } },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun JourneyPlaceSelector(
    title: String,
    places: List<PlaceEntity>,
    selectedId: String,
    onSelected: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            maxItemsInEachRow = 3,
        ) {
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
