package com.mascode.itineraire.ui.today

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mascode.itineraire.domain.model.DayEventType

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuickActionsScreen(
    viewModel: TodayViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var label by remember { mutableStateOf("") }
    var eventType by remember { mutableStateOf(DayEventType.ACTIVITY) }
    var placeId by remember { mutableStateOf<String?>(null) }
    var notes by remember { mutableStateOf("") }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Actions rapides") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Retour")
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
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
                    "Réveil et Sortie maison sont toujours disponibles. Ajoutez ici vos propres raccourcis : un appui enregistrera immédiatement l'événement correspondant.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (state.quickActions.isNotEmpty()) {
                item { Text("Mes actions", style = MaterialTheme.typography.titleMedium) }
                state.quickActions.forEach { action ->
                    item(key = action.id) {
                        Card(Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(action.label, style = MaterialTheme.typography.titleMedium)
                                    Text(eventTypeLabel(action.eventType), style = MaterialTheme.typography.bodySmall)
                                    action.placeId?.let { selectedPlaceId ->
                                        state.places.firstOrNull { it.id == selectedPlaceId }?.let { place ->
                                            Text(place.name, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                                IconButton(onClick = { viewModel.deleteQuickAction(action) }) {
                                    Icon(
                                        Icons.Outlined.Delete,
                                        contentDescription = "Supprimer ${action.label}",
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                }
                            }
                        }
                    }
                }
            }
            item { HorizontalDivider() }
            item { Text("Nouvelle action", style = MaterialTheme.typography.titleLarge) }
            item {
                OutlinedTextField(
                    value = label,
                    onValueChange = {
                        label = it
                        viewModel.clearError()
                    },
                    label = { Text("Nom du raccourci") },
                    placeholder = { Text("Ex. Arrivée au travail") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
            item {
                Text("Événement enregistré", style = MaterialTheme.typography.titleMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    maxItemsInEachRow = 3,
                ) {
                    DayEventType.entries.forEach { type ->
                        FilterChip(
                            selected = eventType == type,
                            onClick = { eventType = type },
                            label = { Text(eventTypeLabel(type)) },
                        )
                    }
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
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                )
            }
            state.errorMessage?.let { message ->
                item { Text(message, color = MaterialTheme.colorScheme.error) }
            }
            item {
                Button(
                    onClick = {
                        viewModel.addQuickAction(
                            label = label,
                            eventType = eventType,
                            placeId = placeId,
                            notes = notes,
                            onSaved = {
                                label = ""
                                eventType = DayEventType.ACTIVITY
                                placeId = null
                                notes = ""
                            },
                        )
                    },
                    enabled = label.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Ajouter l'action")
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}
