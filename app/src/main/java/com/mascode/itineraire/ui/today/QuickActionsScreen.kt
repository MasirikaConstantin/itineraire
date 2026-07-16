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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mascode.itineraire.domain.model.DayEventType
import com.mascode.itineraire.data.local.entity.QuickActionEntity

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
    var editedAction by remember { mutableStateOf<QuickActionEntity?>(null) }
    var actionToDelete by remember { mutableStateOf<QuickActionEntity?>(null) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val labelFocusRequester = remember { FocusRequester() }

    LaunchedEffect(state.isLoading, state.quickActions.isEmpty()) {
        if (!state.isLoading && state.quickActions.isEmpty()) {
            labelFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }

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
                                IconButton(onClick = {
                                    editedAction = action
                                    label = action.label
                                    eventType = action.eventType
                                    placeId = action.placeId
                                    notes = action.notes.orEmpty()
                                }) {
                                    Icon(Icons.Outlined.Edit, contentDescription = "Modifier ${action.label}")
                                }
                                IconButton(onClick = { actionToDelete = action }) {
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
            item {
                Text(
                    if (editedAction == null) "Nouvelle action" else "Modifier l'action",
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            item {
                OutlinedTextField(
                    value = label,
                    onValueChange = {
                        label = it
                        viewModel.clearError()
                    },
                    label = { Text("Nom du raccourci") },
                    placeholder = { Text("Ex. Arrivée au travail") },
                    modifier = Modifier.fillMaxWidth().focusRequester(labelFocusRequester),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
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
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    minLines = 2,
                )
            }
            state.errorMessage?.let { message ->
                item { Text(message, color = MaterialTheme.colorScheme.error) }
            }
            item {
                Button(
                    onClick = {
                        val resetForm = {
                                label = ""
                                eventType = DayEventType.ACTIVITY
                                placeId = null
                                notes = ""
                                editedAction = null
                        }
                        editedAction?.let { action ->
                            viewModel.updateQuickAction(action, label, eventType, placeId, notes, resetForm)
                        } ?: viewModel.addQuickAction(label, eventType, placeId, notes, resetForm)
                    },
                    enabled = label.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (editedAction == null) "Ajouter l'action" else "Enregistrer les modifications")
                }
            }
            if (editedAction != null) {
                item {
                    TextButton(
                        onClick = {
                            label = ""
                            eventType = DayEventType.ACTIVITY
                            placeId = null
                            notes = ""
                            editedAction = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Annuler la modification") }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    actionToDelete?.let { action ->
        AlertDialog(
            onDismissRequest = { actionToDelete = null },
            title = { Text("Supprimer cette action ?") },
            text = { Text("Le raccourci « ${action.label} » sera définitivement supprimé.") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    viewModel.deleteQuickAction(action)
                    if (editedAction?.id == action.id) editedAction = null
                    actionToDelete = null
                }) { Text("Supprimer", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { actionToDelete = null }) { Text("Annuler") }
            },
        )
    }
}
