package com.mascode.itineraire.ui.places

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mascode.itineraire.domain.model.PlaceCategory

@Composable
fun PlacesScreen(viewModel: PlacesViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            Text("Mes lieux", style = MaterialTheme.typography.headlineMedium)
            Text("Enregistrez Maison, ISC, travail ou vos arrêts habituels.")
            Spacer(Modifier.height(12.dp))
            Button(onClick = { showDialog = true }, modifier = Modifier.fillMaxWidth()) { Text("Ajouter un lieu") }
        }
        state.errorMessage?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
        if (state.places.isEmpty()) {
            item { Text("Aucun lieu enregistré.") }
        } else {
            items(state.places, key = { it.id }) { place ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(place.name, style = MaterialTheme.typography.titleMedium)
                        Text(categoryLabel(place.category), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }

    if (showDialog) {
        AddPlaceDialog(
            onDismiss = { showDialog = false },
            onConfirm = { name, category ->
                viewModel.addPlace(name, category)
                showDialog = false
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddPlaceDialog(onDismiss: () -> Unit, onConfirm: (String, PlaceCategory) -> Unit) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(PlaceCategory.OTHER) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajouter un lieu") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Catégorie", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    maxItemsInEachRow = 3,
                ) {
                    PlaceCategory.entries.forEach { item ->
                        FilterChip(
                            selected = category == item,
                            onClick = { category = item },
                            label = { Text(categoryLabel(item)) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name, category) }, enabled = name.isNotBlank()) { Text("Ajouter") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}

private fun categoryLabel(category: PlaceCategory) = when (category) {
    PlaceCategory.HOME -> "Maison"
    PlaceCategory.SCHOOL -> "École"
    PlaceCategory.UNIVERSITY -> "Université"
    PlaceCategory.WORK -> "Travail"
    PlaceCategory.CHURCH -> "Église"
    PlaceCategory.TRANSPORT_STOP -> "Arrêt"
    PlaceCategory.MARKET -> "Marché / commerce"
    PlaceCategory.HEALTH -> "Santé"
    PlaceCategory.ADMINISTRATION -> "Administration"
    PlaceCategory.RESTAURANT -> "Restaurant"
    PlaceCategory.LEISURE -> "Loisirs"
    PlaceCategory.FAMILY_FRIEND -> "Famille / ami"
    PlaceCategory.OTHER -> "Autre"
}
