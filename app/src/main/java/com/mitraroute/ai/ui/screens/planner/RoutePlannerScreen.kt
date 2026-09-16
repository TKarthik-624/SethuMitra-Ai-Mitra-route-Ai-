package com.mitraroute.ai.ui.screens.planner

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.activity.compose.BackHandler
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.mitraroute.ai.data.model.PlacePrediction
import com.mitraroute.ai.ui.components.RiskBadge
import com.mitraroute.ai.ui.theme.*
import com.mitraroute.ai.util.LocationTracker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutePlannerScreen(
    viewModel: RoutePlannerViewModel = viewModel(),
    onBack: () -> Unit = {},
    onNavigateToAI: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    BackHandler { onBack() }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(26.14, 91.73), 6f)
    }

    // Center map on user location when available and no route is active
    LaunchedEffect(state.currentLatLng) {
        if (state.routes.isEmpty() && state.currentLatLng != null) {
            cameraPositionState.position = CameraPosition.fromLatLngZoom(
                LatLng(state.currentLatLng!!.lat, state.currentLatLng!!.lng),
                12f
            )
        }
    }

    LaunchedEffect(state.routes, state.selectedRouteIndex) {
        if (state.routes.isNotEmpty()) {
            val points = LocationTracker.decodePolyline(state.routes[state.selectedRouteIndex].polylinePoints)
            if (points.isNotEmpty()) {
                cameraPositionState.position = CameraPosition.fromLatLngZoom(points.first(), 8f)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ROUTE PLANNER", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg)
            )
        },
        containerColor = DarkBg
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 1. Search Box
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SearchField(
                        value = state.fromQuery,
                        onValueChange = { viewModel.updateFromQuery(it) },
                        placeholder = "Starting from...",
                        icon = Icons.Default.TripOrigin
                    )
                    if (state.fromPredictions.isNotEmpty()) {
                        PredictionsList(state.fromPredictions) { viewModel.selectFrom(it) }
                    }

                    Box(modifier = Modifier.fillMaxWidth().height(12.dp)) {
                        IconButton(
                            onClick = { viewModel.swapLocations() },
                            modifier = Modifier.align(Alignment.Center).size(24.dp)
                        ) {
                            Icon(Icons.Default.SwapVert, contentDescription = "Swap", tint = Emerald400, modifier = Modifier.size(16.dp))
                        }
                    }

                    SearchField(
                        value = state.toQuery,
                        onValueChange = { viewModel.updateToQuery(it) },
                        placeholder = "Destination...",
                        icon = Icons.Default.Place
                    )
                    if (state.toPredictions.isNotEmpty()) {
                        PredictionsList(state.toPredictions) { viewModel.selectTo(it) }
                    }

                    Button(
                        onClick = { viewModel.checkRoute() },
                        enabled = state.canCheckRoute && !state.isLoading,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald500),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Slate950)
                        } else {
                            Text("CHECK ROUTE", fontWeight = FontWeight.Black, color = Slate950)
                        }
                    }
                }
            }

            // 2. Map & Results
            Box(modifier = Modifier.weight(1f)) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(
                        mapType = MapType.HYBRID,
                        isMyLocationEnabled = hasLocationPermission
                    ),
                    uiSettings = MapUiSettings(zoomControlsEnabled = false)
                ) {
                    state.selectedFrom?.let { from ->
                        Marker(
                            state = MarkerState(position = LatLng(from.geometry.location.lat, from.geometry.location.lng)),
                            title = "Start: ${from.name}",
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                        )
                    }
                    state.selectedTo?.let { to ->
                        Marker(
                            state = MarkerState(position = LatLng(to.geometry.location.lat, to.geometry.location.lng)),
                            title = "Destination: ${to.name}",
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                        )
                    }

                    state.routes.forEachIndexed { index, route ->
                        val isSelected = index == state.selectedRouteIndex
                        val points = remember(route.polylinePoints) { 
                            LocationTracker.decodePolyline(route.polylinePoints)
                        }
                        if (points.isNotEmpty()) {
                            Polyline(
                                points = points,
                                color = if (isSelected) Color(android.graphics.Color.parseColor(route.colorHex)) else Color.Gray.copy(alpha = 0.4f),
                                width = if (isSelected) 12f else 6f,
                                zIndex = if (isSelected) 1f else 0f
                            )
                        }
                    }
                }

                // Locate Me Button
                FloatingActionButton(
                    onClick = { viewModel.useCurrentLocationAsOrigin() },
                    modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).size(40.dp),
                    containerColor = DarkCard,
                    contentColor = Emerald400
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = "Locate Me", modifier = Modifier.size(20.dp))
                }

                // Error Overlay
                state.error?.let { err ->
                    Card(
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkCard.copy(alpha = 0.9f)),
                        border = BorderStroke(1.dp, Rose400)
                    ) {
                        Text(err, color = Rose400, modifier = Modifier.padding(16.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                    }
                }

                // Results Card (Bottom)
                if (state.routes.isNotEmpty()) {
                    val active = state.routes[state.selectedRouteIndex]
                    Card(
                        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkCard),
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                        border = BorderStroke(1.dp, DarkBorder)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text(active.name, fontWeight = FontWeight.Black, color = TextPrimary)
                                    Text("${active.distanceText} • ${active.durationText}", color = TextMuted, fontSize = 14.sp)
                                }
                                RiskBadge(riskLevel = active.riskLevel.lowercase())
                            }
                            HorizontalDivider(color = DarkBorder)
                            Text("WEATHER: ${active.weatherSummary}", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                            
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                state.routes.forEachIndexed { index, _ ->
                                    Button(
                                        onClick = { viewModel.selectRoute(index) },
                                        modifier = Modifier.weight(1f).height(36.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = if(index == state.selectedRouteIndex) Emerald500 else DarkInput),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("Alt ${index + 1}", fontSize = 10.sp)
                                    }
                                }
                                IconButton(onClick = onNavigateToAI, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Default.AutoAwesome, null, tint = Emerald400)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: ImageVector
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = TextMuted) },
        leadingIcon = { Icon(icon, null, tint = Emerald400) },
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = DarkInput,
            unfocusedContainerColor = DarkInput,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary
        ),
        singleLine = true
    )
}

@Composable
private fun PredictionsList(results: List<PlacePrediction>, onSelect: (PlacePrediction) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().heightIn(max = 160.dp),
        colors = CardDefaults.cardColors(containerColor = DarkInput),
        border = BorderStroke(1.dp, DarkBorder)
    ) {
        LazyColumn {
            items(results) { res ->
                Text(
                    text = res.description,
                    modifier = Modifier.fillMaxWidth().clickable { onSelect(res) }.padding(12.dp),
                    color = TextPrimary,
                    style = MaterialTheme.typography.bodySmall
                )
                HorizontalDivider(color = DarkBorder)
            }
        }
    }
}


