package dev.gabay.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.gabay.app.ui.GabayColors
import dev.gabay.app.ui.GabayTheme
import dev.gabay.app.ui.GabayTypography
import org.maplibre.android.MapLibre

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize MapLibre Native instance
        MapLibre.getInstance(this)
        
        enableEdgeToEdge()
        setContent {
            GabayTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = GabayColors.BgObsidian
                ) { innerPadding ->
                    CockpitScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun CockpitScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(GabayColors.BgObsidian),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "GABAY COCKPIT ENGINE READY",
            style = GabayTypography.StreetName,
            color = GabayColors.AccentEmerald
        )
    }
}
