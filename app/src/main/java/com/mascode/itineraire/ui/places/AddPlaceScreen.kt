package com.mascode.itineraire.ui.places

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mascode.itineraire.domain.model.PlaceCategory
import kotlinx.coroutines.launch
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.style.BaseStyle
import org.maplibre.compose.util.ClickResult
import org.maplibre.spatialk.geojson.Position
import java.util.Locale

private val KINSHASA_POSITION = Position(latitude = -4.325, longitude = 15.322)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddPlaceScreen(
    viewModel: PlacesViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(PlaceCategory.OTHER) }
    var includePosition by remember { mutableStateOf(false) }
    var locationMessage by remember { mutableStateOf<String?>(null) }
    var mapMessage by remember { mutableStateOf<String?>(null) }
    val cameraState = rememberCameraState(
        firstPosition = CameraPosition(target = KINSHASA_POSITION, zoom = 12.0),
    )

    fun centerOn(location: Location) {
        includePosition = true
        locationMessage = null
        coroutineScope.launch {
            cameraState.animateTo(
                cameraState.position.copy(
                    target = Position(latitude = location.latitude, longitude = location.longitude),
                    zoom = 16.0,
                ),
            )
        }
    }

    fun loadCurrentLocation() {
        findCurrentLocation(
            context = context,
            onLocation = ::centerOn,
            onError = { locationMessage = it },
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        if (grants.values.any { it }) {
            loadCurrentLocation()
        } else {
            locationMessage = "La permission a été refusée. Vous pouvez toujours choisir le lieu sur la carte."
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Ajouter un lieu") },
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
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        viewModel.clearError()
                    },
                    label = { Text("Nom du lieu") },
                    placeholder = { Text("Ex. Maison, ISC, Rond-point Ngaba") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
            item {
                Text("Catégorie", style = MaterialTheme.typography.titleMedium)
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
            item {
                Text("Position facultative", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Elle permettra de calculer les distances plus tard. Aucun suivi en arrière-plan n'est effectué.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (!includePosition) {
                item {
                    OutlinedButton(
                        onClick = { includePosition = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Outlined.LocationOn, contentDescription = null)
                        Text("  Choisir sur la carte")
                    }
                }
            } else {
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(320.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                            ) {
                                MaplibreMap(
                                    modifier = Modifier.fillMaxSize(),
                                    baseStyle = BaseStyle.Uri("https://tiles.openfreemap.org/styles/liberty"),
                                    cameraState = cameraState,
                                    onMapClick = { position, _ ->
                                        cameraState.position = cameraState.position.copy(target = position)
                                        ClickResult.Consume
                                    },
                                    onMapLoadFailed = { reason ->
                                        mapMessage = reason ?: "Impossible de charger la carte."
                                    },
                                )
                                Icon(
                                    imageVector = Icons.Outlined.LocationOn,
                                    contentDescription = "Position sélectionnée",
                                    modifier = Modifier.align(Alignment.Center).size(44.dp),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                            Text(
                                "Déplacez la carte pour placer l'épingle au bon endroit.",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Text(
                                formatCoordinates(cameraState.position.target),
                                style = MaterialTheme.typography.labelMedium,
                            )
                            mapMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                            OutlinedButton(
                                onClick = {
                                    if (hasLocationPermission(context)) {
                                        loadCurrentLocation()
                                    } else {
                                        permissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                            ),
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(Icons.Outlined.MyLocation, contentDescription = null)
                                Text("  Utiliser ma position actuelle")
                            }
                            TextButton(
                                onClick = {
                                    includePosition = false
                                    locationMessage = null
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Retirer la position")
                            }
                        }
                    }
                }
            }

            locationMessage?.let { message ->
                item { Text(message, color = MaterialTheme.colorScheme.error) }
            }
            state.errorMessage?.let { message ->
                item { Text(message, color = MaterialTheme.colorScheme.error) }
            }
            item {
                Button(
                    onClick = {
                        val position = cameraState.position.target.takeIf { includePosition }
                        viewModel.addPlace(
                            name = name,
                            category = category,
                            latitude = position?.latitude,
                            longitude = position?.longitude,
                            onSaved = onBack,
                        )
                    },
                    enabled = name.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Enregistrer le lieu")
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

private fun hasLocationPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED

@SuppressLint("MissingPermission")
private fun findCurrentLocation(
    context: Context,
    onLocation: (Location) -> Unit,
    onError: (String) -> Unit,
) {
    if (!hasLocationPermission(context)) {
        onError("La permission de localisation est nécessaire pour utiliser la position actuelle.")
        return
    }
    val manager = context.getSystemService(LocationManager::class.java)
    val provider = when {
        manager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
        manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
        else -> {
            onError("Activez la localisation du téléphone, ou choisissez la position manuellement sur la carte.")
            return
        }
    }
    manager.getCurrentLocation(provider, CancellationSignal(), context.mainExecutor) { location ->
        if (location != null) onLocation(location) else onError("La position actuelle n'est pas disponible.")
    }
}

private fun formatCoordinates(position: Position): String = String.format(
    Locale.FRENCH,
    "Latitude %.6f · Longitude %.6f",
    position.latitude,
    position.longitude,
)
