package dev.gabay.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService

/**
 * Immortal 1Hz GPS Navigation ForegroundService
 * Continuously polls location updates and pipes them into rust_core road snapping.
 */
class NavigationService : LifecycleService() {

    companion object {
        const val CHANNEL_ID = "gabay_nav_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START_NAV = "dev.gabay.app.action.START_NAV"
        const val ACTION_STOP_NAV = "dev.gabay.app.action.STOP_NAV"

        var onStopNavigationRequested: (() -> Unit)? = null

        fun start(context: Context) {
            val hasFine = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            val hasCoarse = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

            if (!hasFine && !hasCoarse) {
                android.util.Log.w("NavigationService", "Cannot start NavigationService: Location permission not granted")
                return
            }

            val intent = Intent(context, NavigationService::class.java).apply {
                action = ACTION_START_NAV
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                android.util.Log.e("NavigationService", "Failed to start service: ${e.message}")
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, NavigationService::class.java).apply {
                action = ACTION_STOP_NAV
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_START_NAV -> startForegroundServiceInternal()
            ACTION_STOP_NAV -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(true)
                }
                onStopNavigationRequested?.invoke()
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun startForegroundServiceInternal() {
        try {
            val hasFine = androidx.core.content.ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            val hasCoarse = androidx.core.content.ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

            if (!hasFine && !hasCoarse) {
                android.util.Log.w("NavigationService", "Location permission missing for FGS location type. Stopping service.")
                stopSelf()
                return
            }

            val notification = buildNotification("Gabay Navigation Active", "Monitoring road telemetry & GPS...")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: SecurityException) {
            android.util.Log.e("NavigationService", "SecurityException during startForeground: ${e.message}", e)
            stopSelf()
        } catch (e: Exception) {
            android.util.Log.e("NavigationService", "Unexpected exception during startForeground: ${e.message}", e)
            stopSelf()
        }
    }

    private fun buildNotification(title: String, content: String): Notification {
        val launchIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, NavigationService::class.java).apply {
            action = ACTION_STOP_NAV
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_nav_cursor)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Stop Navigation",
                stopPendingIntent
            )
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Gabay Live Navigation",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Live turn-by-turn cockpit telemetry channel"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
}
