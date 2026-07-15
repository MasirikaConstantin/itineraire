package com.mascode.itineraire.ui.places

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mascode.itineraire.domain.model.PlaceCategory

@Composable
fun PlacesScreen(
    viewModel: PlacesViewModel,
    onAddPlace: () -> Unit,
    onEditPlace: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            Text("Mes lieux", style = MaterialTheme.typography.headlineMedium)
            Text("Enregistrez vos sources (Ex : Maison)ou vos arrêts habituels(Ex : Rond - Point,...).")
            Spacer(Modifier.height(12.dp))
            Button(onClick = onAddPlace, modifier = Modifier.fillMaxWidth()) { Text("Ajouter un lieu") }
        }
        state.errorMessage?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
        if (state.places.isEmpty()) {
            item { Text("Aucun lieu enregistré.") }
        } else {
            items(state.places, key = { it.id }) { place ->
                Card(
                    onClick = { onEditPlace(place.id) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(place.name, style = MaterialTheme.typography.titleMedium)
                        Text(categoryLabel(place.category), style = MaterialTheme.typography.bodySmall)
                        if (place.latitude != null && place.longitude != null) {
                            Text("Position enregistrée", style = MaterialTheme.typography.bodySmall)
                        }
                        Text("Appuyez pour modifier", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }

}

internal fun categoryLabel(category: PlaceCategory) = when (category) {
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
