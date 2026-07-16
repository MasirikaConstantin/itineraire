package com.mascode.itineraire.ui.notification

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import androidx.core.content.ContextCompat
import com.mascode.itineraire.ItineraireApplication
import com.mascode.itineraire.MainActivity
import com.mascode.itineraire.R
import com.mascode.itineraire.data.local.entity.PlaceEntity
import com.mascode.itineraire.ui.widget.updateJourneyWidgets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class JourneyNotificationManager(
    private val context: Context,
) {
    private val container
        get() = (context.applicationContext as ItineraireApplication).container

    private val notificationManager
        get() = context.getSystemService(NotificationManager::class.java)

    suspend fun hasActiveLeg(): Boolean = container.journeyRepository.getWidgetJourneyData()?.activeLeg != null

    suspend fun synchronize() {
        createChannel()
        val data = container.journeyRepository.getWidgetJourneyData()
        val activeLeg = data?.activeLeg
        if (data == null || activeLeg == null || !canPostNotifications()) {
            notificationManager.cancel(NOTIFICATION_ID)
            return
        }

        val protected = container.appSecurityRepository.isBiometricLockEnabled()
        val places = container.placeRepository.getAll().associateBy(PlaceEntity::id)
        val source = places[data.journey.sourcePlaceId]?.name ?: "Lieu inconnu"
        val destination = places[data.journey.destinationPlaceId]?.name ?: "Lieu inconnu"
        val legSource = places[activeLeg.sourcePlaceId]?.name ?: "Lieu inconnu"
        val legDestination = places[activeLeg.destinationPlaceId]?.name ?: "Lieu inconnu"
        val nextLeg = data.nextPlannedLeg
        val nextDestination = nextLeg?.let { places[it.destinationPlaceId]?.name ?: "Lieu inconnu" }

        val contentIntent = PendingIntent.getActivity(
            context,
            OPEN_REQUEST_CODE,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_route)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setAutoCancel(false)
            .setCategory(Notification.CATEGORY_PROGRESS)
            .setColor(Color.rgb(139, 233, 253))
            .setShowWhen(true)
            .setWhen(activeLeg.startedAt.toEpochMilli())
            .setUsesChronometer(true)

        if (Build.VERSION.SDK_INT_FULL >= Build.VERSION_CODES_FULL.BAKLAVA_1) {
            builder
                .setRequestPromotedOngoing(true)
                .setShortCriticalText(if (protected) "Trajet" else legDestination.take(7))
        }

        if (protected) {
            builder
                .setContentTitle("Trajet en cours")
                .setContentText("Données masquées · ouvrez l'application pour vous authentifier")
                .setVisibility(Notification.VISIBILITY_SECRET)
        } else {
            val details = buildString {
                append("$legSource → $legDestination")
                if (nextDestination != null) append(" · Ensuite : $nextDestination")
            }
            builder
                .setContentTitle("$source → $destination")
                .setContentText(details)
                .setStyle(Notification.BigTextStyle().bigText(details))
                .setSubText("Tronçon en cours")
                .setVisibility(Notification.VISIBILITY_PRIVATE)
                .addAction(advanceAction(hasNextLeg = nextLeg != null))
        }

        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    fun cancel() {
        notificationManager.cancel(NOTIFICATION_ID)
    }

    private fun advanceAction(hasNextLeg: Boolean): Notification.Action {
        val intent = Intent(context, JourneyNotificationReceiver::class.java).apply {
            action = ACTION_ADVANCE
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ADVANCE_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return Notification.Action.Builder(
            null,
            if (hasNextLeg) "Terminer et continuer" else "Terminer le tronçon",
            pendingIntent,
        )
            .setAuthenticationRequired(true)
            .build()
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Trajet en cours",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Affiche le tronçon actif et permet de passer au suivant."
            setSound(null, null)
            enableVibration(false)
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun canPostNotifications(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    private companion object {
        const val CHANNEL_ID = "active_journey"
        const val NOTIFICATION_ID = 1042
        const val OPEN_REQUEST_CODE = 1043
        const val ADVANCE_REQUEST_CODE = 1044
        const val ACTION_ADVANCE = "com.mascode.itineraire.action.ADVANCE_JOURNEY"
    }
}

class JourneyNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_ADVANCE) return
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val application = context.applicationContext as ItineraireApplication
                val container = application.container
                if (!container.appSecurityRepository.isBiometricLockEnabled()) {
                    val journeyId = container.journeyRepository.getWidgetJourneyData()?.journey?.id
                    if (journeyId != null) {
                        runCatching {
                            container.journeyRepository.finishActiveLegAndStartNext(journeyId)
                        }
                    }
                }
                container.journeyNotificationManager.synchronize()
                updateJourneyWidgets(context)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        const val ACTION_ADVANCE = "com.mascode.itineraire.action.ADVANCE_JOURNEY"
    }
}
