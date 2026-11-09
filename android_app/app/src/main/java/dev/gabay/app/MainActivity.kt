package dev.gabay.app

import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import dev.gabay.app.core.PermissionManager
import dev.gabay.app.ui.MainScreen
import dev.gabay.app.ui.theme.GabayTheme
import dev.gabay.app.viewmodel.MapViewModel

class MainActivity : ComponentActivity(), LocationListener {

    private val viewModel: MapViewModel by viewModels()
    private var locationManager: LocationManager? = null

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.refreshPermissionsAndVault()
        startLocationUpdates()
    }

    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refreshPermissionsAndVault()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        locationManager = getSystemService(Context.LOCATION_SERVICE) as? LocationManager

        setContent {
            val state by viewModel.uiState.collectAsState()
            GabayTheme(darkTheme = state.isDarkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen(
                        viewModel = viewModel,
                        onRequestLocationPermission = {
                            locationPermissionLauncher.launch(PermissionManager.REQUIRED_LOCATION_PERMISSIONS)
                        },
                        onRequestStoragePermission = {
                            val intent = PermissionManager.createManageStorageIntent(this)
                            storagePermissionLauncher.launch(intent)
                        }
                    )
                }
            }
        }

        startLocationUpdates()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshPermissionsAndVault()
        startLocationUpdates()
    }

    override fun onPause() {
        super.onPause()
        locationManager?.removeUpdates(this)
    }

    private fun startLocationUpdates() {
        if (!PermissionManager.hasLocationPermission(this)) return

        try {
            locationManager?.let { lm ->
                if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 1.0f, this)
                }
                if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2000L, 2.0f, this)
                }
                val lastKnown = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                lastKnown?.let { viewModel.onLocationChanged(it) }
            }
        } catch (e: SecurityException) {
            // Permission revoked
        }
    }

    override fun onLocationChanged(location: Location) {
        viewModel.onLocationChanged(location)
    }

    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
}
