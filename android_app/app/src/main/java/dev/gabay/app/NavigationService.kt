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
        const val CHANNEL_ID = "gabay_live_nav_v2"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START_NAV = "dev.gabay.app.action.START_NAV"
        const val ACTION_STOP_NAV = "dev.gabay.app.action.STOP_NAV"
        const val ACTION_STOP_FROM_NOTIFICATION = "dev.gabay.app.action.STOP_FROM_NOTIFICATION"
        const val ACTION_UPDATE_NOTIFICATION = "dev.gabay.app.action.UPDATE_NOTIFICATION"

        var onStopNavigationRequested: (() -> Unit)? = null

        fun start(context: Context, title: String = "Gabay Cockpit Active", content: String = "Monitoring road telemetry & GPS...") {
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
                putExtra("extra_title", title)
                putExtra("extra_content", content)
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

        fun update(context: Context, title: String, content: String) {
            val intent = Intent(context, NavigationService::class.java).apply {
                action = ACTION_UPDATE_NOTIFICATION
                putExtra("extra_title", title)
                putExtra("extra_content", content)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {}
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
            ACTION_START_NAV -> {
                val title = intent.getStringExtra("extra_title") ?: "Gabay Cockpit Active"
                val content = intent.getStringExtra("extra_content") ?: "Monitoring road telemetry & GPS..."
                startForegroundServiceInternal(title, content)
            }
            ACTION_UPDATE_NOTIFICATION -> {
                val title = intent.getStringExtra("extra_title") ?: "Gabay Navigation Active"
                val content = intent.getStringExtra("extra_content") ?: "Monitoring road telemetry & GPS..."
                updateStatus(title, content)
            }
            ACTION_STOP_FROM_NOTIFICATION -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(true)
                }
                onStopNavigationRequested?.invoke()
                stopSelf()
            }
            ACTION_STOP_NAV -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(true)
                }
                // Do not invoke onStopNavigationRequested: caller (MainActivity) already requested this stop!
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun startForegroundServiceInternal(title: String, content: String) {
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

            val notification = buildNotification(title, content)
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
            action = ACTION_STOP_FROM_NOTIFICATION
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
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pendingIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Stop Navigation",
                stopPendingIntent
            )
            .build()
    }

    fun updateStatus(title: String, content: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID, buildNotification(title, content))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Gabay Live Navigation",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Live turn-by-turn cockpit telemetry channel"
                setShowBadge(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
}
