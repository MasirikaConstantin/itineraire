package com.mascode.itineraire.ui.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import com.mascode.itineraire.ItineraireApplication
import com.mascode.itineraire.MainActivity
import com.mascode.itineraire.R
import com.mascode.itineraire.data.local.entity.PlaceEntity
import com.mascode.itineraire.data.repository.JourneyRepository.WidgetJourneyData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class JourneyWidgetState(
    val protected: Boolean,
    val journeyId: String? = null,
    val route: String? = null,
    val activeLeg: String? = null,
    val nextLeg: String? = null,
    val startedAt: Instant? = null,
)

class JourneyWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = withContext(Dispatchers.IO) { loadWidgetState(context) }
        provideContent { JourneyWidgetContent(state) }
    }
}

class JourneyWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = JourneyWidget()
}

class AdvanceJourneyAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val container = context.appContainer
        if (container.appSecurityRepository.isBiometricLockEnabled()) return
        val journeyId = parameters[JOURNEY_ID] ?: return
        runCatching { container.journeyRepository.finishActiveLegAndStartNext(journeyId) }
        JourneyWidget().updateAll(context)
    }
}

suspend fun updateJourneyWidgets(context: Context) {
    JourneyWidget().updateAll(context.applicationContext)
}

private suspend fun loadWidgetState(context: Context): JourneyWidgetState {
    val container = context.appContainer
    if (container.appSecurityRepository.isBiometricLockEnabled()) {
        return JourneyWidgetState(protected = true)
    }
    val data = container.journeyRepository.getWidgetJourneyData()
        ?: return JourneyWidgetState(protected = false)
    val places = container.placeRepository.getAll().associateBy(PlaceEntity::id)
    return data.toWidgetState(places)
}

private fun WidgetJourneyData.toWidgetState(places: Map<String, PlaceEntity>): JourneyWidgetState = JourneyWidgetState(
    protected = false,
    journeyId = journey.id,
    route = "${places[journey.sourcePlaceId]?.name.orUnknown()} → ${places[journey.destinationPlaceId]?.name.orUnknown()}",
    activeLeg = activeLeg?.let {
        "${places[it.sourcePlaceId]?.name.orUnknown()} → ${places[it.destinationPlaceId]?.name.orUnknown()}"
    },
    nextLeg = nextPlannedLeg?.let {
        "${places[it.sourcePlaceId]?.name.orUnknown()} → ${places[it.destinationPlaceId]?.name.orUnknown()}"
    },
    startedAt = activeLeg?.startedAt ?: journey.startedAt,
)

@Composable
private fun JourneyWidgetContent(state: JourneyWidgetState) {
    val size = LocalSize.current
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(WIDGET_BACKGROUND)
            .padding(16.dp)
            .clickable(actionStartActivity(Intent(androidx.glance.LocalContext.current, MainActivity::class.java))),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
            Image(
                provider = ImageProvider(R.drawable.ic_widget_route),
                contentDescription = null,
                modifier = GlanceModifier.size(26.dp),
            )
            Spacer(GlanceModifier.width(8.dp))
            Text(
                "Itinéraire",
                style = TextStyle(
                    color = WIDGET_PRIMARY,
                    fontWeight = FontWeight.Bold,
                ),
            )
        }
        Spacer(GlanceModifier.height(10.dp))
        when {
            state.protected -> ProtectedWidgetContent()
            state.journeyId == null -> EmptyWidgetContent()
            else -> ActiveWidgetContent(state, compact = size.height < 130.dp)
        }
    }
}

@Composable
private fun ProtectedWidgetContent() {
    Text(
        "Données masquées",
        style = TextStyle(color = WIDGET_TEXT, fontWeight = FontWeight.Bold),
    )
    Text("Ouvrez l'application pour vous authentifier.", style = TextStyle(color = WIDGET_MUTED))
}

@Composable
private fun EmptyWidgetContent() {
    Text(
        "Aucun trajet en cours",
        style = TextStyle(color = WIDGET_TEXT, fontWeight = FontWeight.Bold),
    )
    Text("Touchez pour ouvrir l'application.", style = TextStyle(color = WIDGET_MUTED))
}

@Composable
private fun ActiveWidgetContent(state: JourneyWidgetState, compact: Boolean) {
    Text(
        state.route.orEmpty(),
        maxLines = 1,
        style = TextStyle(color = WIDGET_TEXT, fontWeight = FontWeight.Bold),
    )
    val startedAt = state.startedAt?.let { WIDGET_TIME_FORMATTER.format(it.atZone(ZoneId.systemDefault())) }
    state.activeLeg?.let {
        Text("En cours · $it${startedAt?.let { value -> " · depuis $value" }.orEmpty()}", maxLines = 1,
            style = TextStyle(color = WIDGET_MUTED))
    } ?: Text("Aucun tronçon actif", style = TextStyle(color = WIDGET_MUTED))

    if (!compact) {
        state.nextLeg?.let {
            Spacer(GlanceModifier.height(5.dp))
            Text("Ensuite · $it", maxLines = 1, style = TextStyle(color = WIDGET_MUTED))
        }
    }
    Spacer(GlanceModifier.height(10.dp))
    if (state.activeLeg != null) {
        androidx.glance.Button(
            text = if (state.nextLeg == null) "Terminer le tronçon" else "Terminer et continuer",
            onClick = actionRunCallback<AdvanceJourneyAction>(actionParametersOf(JOURNEY_ID to state.journeyId.orEmpty())),
            modifier = GlanceModifier.fillMaxWidth(),
        )
    } else {
        Text("Ouvrez l'application pour commencer un tronçon.", style = TextStyle(color = WIDGET_MUTED))
    }
}

private fun String?.orUnknown(): String = this ?: "Lieu inconnu"

private val Context.appContainer
    get() = (applicationContext as ItineraireApplication).container

private val JOURNEY_ID = ActionParameters.Key<String>("journey_id")
private val WIDGET_BACKGROUND = ColorProvider(Color(0xFF30323E))
private val WIDGET_PRIMARY = ColorProvider(Color(0xFF8BE9FD))
private val WIDGET_TEXT = ColorProvider(Color(0xFFF8F8F2))
private val WIDGET_MUTED = ColorProvider(Color(0xFFC5C8D6))
private val WIDGET_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")
