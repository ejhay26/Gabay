package dev.gabay.app.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.gabay.app.ui.components.*
import dev.gabay.app.ui.theme.CreamBackground
import dev.gabay.app.ui.theme.DarkBackground
import dev.gabay.app.ui.theme.TerracottaAccent
import dev.gabay.app.viewmodel.MapUiState
import dev.gabay.app.viewmodel.MapViewModel

@Composable
fun MainScreen(
    viewModel: MapViewModel,
    onRequestLocationPermission: () -> Unit,
    onRequestStoragePermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (state.isDarkTheme) DarkBackground else CreamBackground)
    ) {
        // 1. Map Canvas / View Container Placeholder for Claude to integrate MapLibre or custom PMTiles painter
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (state.isEngineReady) "Map Canvas Ready\n(${state.vaultStatus.directory.name})" else "Loading offline vault data...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }

        // 2. Floating Search Pill (Top)
        if (!state.isNavigating) {
            SearchPill(
                query = state.searchQuery,
                onQueryChange = viewModel::onSearchQueryChanged,
                results = state.searchResults,
                onSelectPoi = viewModel::selectPoi,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
            )
        }

        // 3. Permission Banner (Top/Center if needed)
        if (!state.permissions.allEssentialGranted) {
            PermissionBanner(
                permissions = state.permissions,
                onRequestLocation = onRequestLocationPermission,
                onRequestStorage = onRequestStoragePermission,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 90.dp)
            )
        }

        // 4. Floating Map Tool Controls (Right edge)
        Surface(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(onClick = { /* Zoom in */ }) {
                    Text("+", style = MaterialTheme.typography.titleLarge)
                }
                IconButton(onClick = { /* Zoom out */ }) {
                    Text("\u2212", style = MaterialTheme.typography.titleLarge)
                }
                IconButton(onClick = viewModel::toggleTheme) {
                    Text(if (state.isDarkTheme) "\u2600" else "\u263E", style = MaterialTheme.typography.titleLarge)
                }
            }
        }

        // 5. Bottom Navigation HUD (When active)
        AnimatedVisibility(
            visible = state.isNavigating,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
        ) {
            NavigationHud(
                telemetry = state.telemetry,
                onStopNavigation = viewModel::stopNavigation
            )
        }

        // 6. Bottom Route Preview Sheet (When POI selected & not navigating)
        AnimatedVisibility(
            visible = state.selectedPoi != null && !state.isNavigating,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
        ) {
            state.selectedPoi?.let { poi ->
                RouteSheet(
                    poi = poi,
                    route = state.activeRoute,
                    isRouting = state.isRouting,
                    onStartNavigation = viewModel::startNavigation,
                    onClose = { viewModel.selectPoi(null) }
                )
            }
        }
    }
}
