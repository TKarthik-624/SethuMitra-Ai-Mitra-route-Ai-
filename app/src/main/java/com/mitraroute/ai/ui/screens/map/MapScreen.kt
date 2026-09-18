package com.mitraroute.ai.ui.screens.map

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AltRoute
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.mitraroute.ai.ui.theme.*
import com.mitraroute.ai.util.LocationTracker

@Composable
fun MapScreen(
    viewModel: MapViewModel = viewModel(),
    onNavigateToPlanner: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(26.14, 91.73), 7f)
    }

    LaunchedEffect(state.userLocation) {
        if (state.selectedRoute == null && state.userLocation != null) {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(state.userLocation!!.lat, state.userLocation!!.lng),
                    14f
                )
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                mapType = MapType.HYBRID,
                isMyLocationEnabled = hasLocationPermission
            )
        ) {
            state.routes.forEach { route ->
                val pos = remember(route.id) { LatLng(route.start_lat, route.start_lon) }
                Marker(
                    state = rememberMarkerState(position = pos),
                    title = route.name,
                    snippet = "Risk: ${route.risk_level}"
                )
            }
            
            state.incidents.forEach { inc ->
                val pos = remember(inc.id) { LatLng(inc.lat, inc.lon) }
                Marker(
                    state = rememberMarkerState(position = pos),
                    title = inc.incident_type.uppercase(),
                    snippet = inc.description
                )
            }

            state.activeModernRoute?.let { route ->
                val points = remember(route.polyline.encodedPolyline) { 
                    LocationTracker.decodePolyline(route.polyline.encodedPolyline)
                }
                if (points.isNotEmpty()) {
                    Polyline(
                        points = points,
                        color = Emerald400,
                        width = 12f
                    )
                }
            }
        }

        // Tactical Overlay (Map Controls)
        MapControls(
            onRecenter = { /* recenter logic handled by isMyLocationEnabled for now */ },
            onNavigateToPlanner = onNavigateToPlanner,
            modifier = Modifier.align(Alignment.CenterEnd)
        )
    }
}

@Composable
private fun MapControls(
    onRecenter: () -> Unit,
    onNavigateToPlanner: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FloatingActionButton(
            onClick = onRecenter,
            containerColor = DarkCard,
            contentColor = Emerald400
        ) {
            Icon(Icons.Default.MyLocation, contentDescription = null)
        }
        
        FloatingActionButton(
            onClick = onNavigateToPlanner,
            containerColor = DarkCard,
            contentColor = Emerald400
        ) {
            Icon(Icons.AutoMirrored.Filled.AltRoute, contentDescription = null)
        }
    }
}
