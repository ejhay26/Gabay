package dev.gabay.app.car

import android.content.Intent
import androidx.car.app.CarAppService
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.model.Action
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.car.app.validation.HostValidator
import java.util.Locale

class GabayCarAppService : CarAppService() {
    override fun createHostValidator(): HostValidator {
        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    }

    override fun onCreateSession(): Session {
        return GabayCarSession()
    }
}

class GabayCarSession : Session() {
    override fun onCreateScreen(intent: Intent): Screen {
        return GabayCarHomeScreen(carContext)
    }
}

class GabayCarHomeScreen(carContext: CarContext) : Screen(carContext) {
    override fun onGetTemplate(): Template {
        val isNav = GabayCarStateBridge.isNavigating
        val paneBuilder = Pane.Builder()

        if (isNav) {
            val maneuver = GabayCarStateBridge.currentManeuverText
            val distM = GabayCarStateBridge.distanceToNextManeuverMeters.toInt()
            val totalKm = GabayCarStateBridge.distanceRemainingKm
            val dest = GabayCarStateBridge.destinationName
            val road = GabayCarStateBridge.currentRoadName

            paneBuilder.addRow(
                Row.Builder()
                    .setTitle(dest)
                    .addText("In ${distM}m: $maneuver")
                    .addText("Current road: ${if (road.isNotBlank()) road else "Mapped Highway"}")
                    .build()
            )

            paneBuilder.addRow(
                Row.Builder()
                    .setTitle("Trip Remaining")
                    .addText("${String.format(Locale.US, "%.1f", totalKm)} km • Offline Routing Active")
                    .build()
            )

            paneBuilder.addAction(
                Action.Builder()
                    .setTitle("End Trip")
                    .setOnClickListener {
                        GabayCarStateBridge.onStopNavigationRequested?.invoke()
                        invalidate()
                    }
                    .build()
            )
        } else {
            paneBuilder.addRow(
                Row.Builder()
                    .setTitle("Gabay Offline Navigation")
                    .addText("Ready to navigate. Tap a recent destination below or select on phone.")
                    .build()
            )

            val recents = dev.gabay.app.data.RecentDestinationsManager.getRecents(carContext).take(3)
            if (recents.isNotEmpty()) {
                for (item in recents) {
                    paneBuilder.addRow(
                        Row.Builder()
                            .setTitle(item.name)
                            .addText(item.subtitle)
                            .setOnClickListener {
                                GabayCarStateBridge.onRecentSelected?.invoke(item.name, item.latitude, item.longitude)
                                invalidate()
                            }
                            .build()
                    )
                }
            } else {
                paneBuilder.addRow(
                    Row.Builder()
                        .setTitle("No Recent Destinations")
                        .addText("Saved destinations and recent trips will appear here.")
                        .build()
                )
            }
        }

        return PaneTemplate.Builder(paneBuilder.build())
            .setHeaderAction(Action.APP_ICON)
            .setTitle(if (isNav) "Gabay Cockpit Navigation" else "Gabay")
            .build()
    }
}
