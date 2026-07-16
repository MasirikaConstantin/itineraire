package com.mascode.itineraire.ui.places

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.outlined.MyLocation
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mascode.itineraire.domain.model.PlaceCategory
import java.util.Locale

private const val LOCATION_TIMEOUT_MILLIS = 12_000L
private const val IMMEDIATE_CACHED_LOCATION_AGE_MILLIS = 30_000L
private const val FALLBACK_CACHED_LOCATION_AGE_MILLIS = 10 * 60_000L

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PlaceEditorScreen(
    viewModel: PlacesViewModel,
    placeId: String? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val existingPlace = state.places.firstOrNull { it.id == placeId }
    var initializedPlaceId by remember(placeId) { mutableStateOf<String?>(null) }
    var name by remember(placeId) { mutableStateOf("") }
    var category by remember(placeId) { mutableStateOf(PlaceCategory.OTHER) }
    var latitude by remember(placeId) { mutableStateOf<Double?>(null) }
    var longitude by remember(placeId) { mutableStateOf<Double?>(null) }
    var locationMessage by remember(placeId) { mutableStateOf<String?>(null) }
    var locationMessageIsError by remember(placeId) { mutableStateOf(false) }
    var isLocating by remember(placeId) { mutableStateOf(false) }

    LaunchedEffect(placeId, existingPlace) {
        if (placeId != null && existingPlace != null && initializedPlaceId != placeId) {
            name = existingPlace.name
            category = existingPlace.category
            latitude = existingPlace.latitude
            longitude = existingPlace.longitude
            initializedPlaceId = placeId
        }
    }

    fun loadCurrentLocation() {
        isLocating = true
        locationMessageIsError = false
        locationMessage = "Recherche de votre position…"
        findCurrentLocation(
            context = context,
            onLocation = { location, fromCache ->
                isLocating = false
                latitude = location.latitude
                longitude = location.longitude
                locationMessage = if (fromCache) {
                    "Une position récente du téléphone a été enregistrée dans le formulaire."
                } else {
                    "Position actuelle enregistrée dans le formulaire."
                }
            },
            onError = {
                isLocating = false
                locationMessageIsError = true
                locationMessage = it
            },
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        if (grants.values.any { it }) {
            loadCurrentLocation()
        } else {
            locationMessageIsError = true
            locationMessage = "La permission de localisation a été refusée. Le lieu peut être enregistré sans position."
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(if (placeId == null) "Ajouter un lieu" else "Modifier le lieu") },
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
                    "Enregistrez la position lorsque vous êtes sur place. Vous pourrez aussi l'ajouter ou la remplacer plus tard.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (latitude != null && longitude != null) {
                item {
                    Text(
                        formatCoordinates(latitude!!, longitude!!),
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Text("Position enregistrée", color = MaterialTheme.colorScheme.primary)
                }
            }
            item {
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
                    enabled = !isLocating,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.MyLocation, contentDescription = null)
                    Text(
                        when {
                            isLocating -> "  Recherche en cours…"
                            latitude == null -> "  Utiliser ma position actuelle"
                            else -> "  Mettre à jour avec ma position actuelle"
                        },
                    )
                }
            }
            if (latitude != null) {
                item {
                    TextButton(
                        onClick = {
                            latitude = null
                            longitude = null
                            locationMessage = null
                            locationMessageIsError = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Retirer la position")
                    }
                }
            }
            locationMessage?.let { message ->
                item {
                    Text(
                        message,
                        color = if (locationMessageIsError) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                    )
                }
            }
            state.errorMessage?.let { message ->
                item { Text(message, color = MaterialTheme.colorScheme.error) }
            }
            item {
                Button(
                    onClick = {
                        viewModel.savePlace(
                            placeId = placeId,
                            name = name,
                            category = category,
                            latitude = latitude,
                            longitude = longitude,
                            onSaved = onBack,
                        )
                    },
                    enabled = name.isNotBlank() && (placeId == null || initializedPlaceId == placeId),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (placeId == null) "Enregistrer le lieu" else "Enregistrer les modifications")
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
    onLocation: (Location, Boolean) -> Unit,
    onError: (String) -> Unit,
) {
    if (!hasLocationPermission(context)) {
        onError("La permission de localisation est nécessaire pour utiliser la position actuelle.")
        return
    }
    val manager = context.getSystemService(LocationManager::class.java)
    if (!manager.isLocationEnabled) {
        onError("Activez la localisation du téléphone pour enregistrer la position actuelle.")
        return
    }

    val providers = listOf(
        LocationManager.FUSED_PROVIDER,
        LocationManager.NETWORK_PROVIDER,
        LocationManager.GPS_PROVIDER,
    ).filter { provider ->
        provider in manager.allProviders && runCatching { manager.isProviderEnabled(provider) }.getOrDefault(false)
    }
    if (providers.isEmpty()) {
        onError("Aucun fournisseur de localisation n'est disponible sur ce téléphone.")
        return
    }

    val cachedLocation = providers
        .mapNotNull { provider -> runCatching { manager.getLastKnownLocation(provider) }.getOrNull() }
        .filter(::isValidLocation)
        .maxByOrNull { it.elapsedRealtimeNanos }
    if (cachedLocation != null && locationAgeMillis(cachedLocation) <= IMMEDIATE_CACHED_LOCATION_AGE_MILLIS) {
        onLocation(cachedLocation, true)
        return
    }

    val handler = Handler(Looper.getMainLooper())
    val signals = providers.map { CancellationSignal() }
    var completed = false
    var remainingProviders = providers.size

    fun complete(location: Location?, fromCache: Boolean = false) {
        if (completed) return
        completed = true
        signals.forEach(CancellationSignal::cancel)
        if (location != null) {
            onLocation(location, fromCache)
        } else {
            onError("Impossible d'obtenir une position récente. Essayez près d'une fenêtre ou à l'extérieur.")
        }
    }

    val timeout = Runnable {
        val fallback = cachedLocation?.takeIf {
            locationAgeMillis(it) <= FALLBACK_CACHED_LOCATION_AGE_MILLIS
        }
        complete(fallback, fromCache = fallback != null)
    }
    handler.postDelayed(timeout, LOCATION_TIMEOUT_MILLIS)

    providers.forEachIndexed { index, provider ->
        manager.getCurrentLocation(provider, signals[index], context.mainExecutor) { location ->
            if (completed) return@getCurrentLocation
            if (location != null && isValidLocation(location)) {
                handler.removeCallbacks(timeout)
                complete(location)
            } else {
                remainingProviders -= 1
                if (remainingProviders == 0) {
                    handler.removeCallbacks(timeout)
                    val fallback = cachedLocation?.takeIf {
                        locationAgeMillis(it) <= FALLBACK_CACHED_LOCATION_AGE_MILLIS
                    }
                    complete(fallback, fromCache = fallback != null)
                }
            }
        }
    }
}

private fun isValidLocation(location: Location): Boolean =
    location.latitude in -90.0..90.0 && location.longitude in -180.0..180.0

private fun locationAgeMillis(location: Location): Long =
    ((SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos) / 1_000_000L).coerceAtLeast(0L)

private fun formatCoordinates(latitude: Double, longitude: Double): String = String.format(
    Locale.FRENCH,
    "Latitude %.6f · Longitude %.6f",
    latitude,
    longitude,
)
