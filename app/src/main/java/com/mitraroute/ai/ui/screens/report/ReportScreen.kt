package com.mitraroute.ai.ui.screens.report

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.mitraroute.ai.R
import com.mitraroute.ai.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    viewModel: ReportViewModel = viewModel(),
    onBack: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    BackHandler { onBack() }
    
    var selectedTypeIndex by remember { mutableIntStateOf(0) }
    var showForm by remember { mutableStateOf(true) }

    val incidentTypes = listOf(
        "road_damage", "flood", "landslide", "bridge_failure",
        "weather_blockage", "vehicle_breakdown", "other"
    )
    val incidentLabels = listOf(
        "🚧 Road Damage", "🌊 Flood", "⛰ Landslide",
        "🌉 Bridge Failure", "🌫 Weather Blockage",
        "🚛 Vehicle Breakdown", "⚠️ Other"
    )

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(state.lat, state.lon), 12f)
    }

    // Auto-center when address changes (selection)
    LaunchedEffect(state.lat, state.lon) {
        cameraPositionState.position = CameraPosition.fromLatLngZoom(LatLng(state.lat, state.lon), 14f)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("REPORT & HAZARDS", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showForm = !showForm }) {
                        Icon(if (showForm) Icons.Default.Map else Icons.Default.AddLocationAlt, null, tint = Emerald400)
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
            if (showForm) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 0.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                    border = BorderStroke(1.dp, DarkBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Incident Type dropdown
                        var typeExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(expanded = typeExpanded, onExpandedChange = { typeExpanded = it }) {
                            OutlinedTextField(
                                value = incidentLabels[selectedTypeIndex],
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Type of Incident") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(typeExpanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                colors = inputColors()
                            )
                            ExposedDropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                                incidentLabels.forEachIndexed { index, label ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = {
                                            selectedTypeIndex = index
                                            viewModel.updateIncidentType(incidentTypes[index])
                                            typeExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Severity picker
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Severity Level", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                (1..5).forEach { sev ->
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (state.severity == sev) Emerald500 else DarkInput)
                                            .clickable { viewModel.updateSeverity(sev) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("$sev", color = if (state.severity == sev) Slate950 else TextPrimary, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = state.description,
                            onValueChange = { viewModel.updateDescription(it) },
                            placeholder = { Text("Describe the situation...", color = TextMuted) },
                            modifier = Modifier.fillMaxWidth().height(80.dp),
                            colors = inputColors()
                        )

                        Button(
                            onClick = { viewModel.submitReport() },
                            enabled = !state.isSubmitting,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Emerald500),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (state.isSubmitting) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Slate950)
                            } else {
                                Text("SUBMIT REPORT", fontWeight = FontWeight.Black, color = Slate950)
                            }
                        }
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(
                        mapType = MapType.HYBRID,
                        isMyLocationEnabled = true
                    ),
                    uiSettings = MapUiSettings(zoomControlsEnabled = false),
                    onMapClick = { latLng ->
                        viewModel.updateLocation(latLng.latitude, latLng.longitude)
                    }
                ) {
                    // Current Marker
                    Marker(
                        state = MarkerState(position = LatLng(state.lat, state.lon)),
                        title = "Report Point",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                    )

                    // Nearby Markers
                    state.nearbyIncidents.forEach { inc ->
                        Marker(
                            state = MarkerState(position = LatLng(inc.lat, inc.lon)),
                            title = inc.incident_type.replace("_", " ").uppercase(),
                            snippet = "Status: ${inc.status}",
                            icon = BitmapDescriptorFactory.defaultMarker(
                                if (inc.severity >= 4) BitmapDescriptorFactory.HUE_RED else BitmapDescriptorFactory.HUE_ORANGE
                            )
                        )
                    }
                }

                // Map Overlay Info
                Column(modifier = Modifier.align(Alignment.BottomCenter)) {
                    // Threat Analysis Panel
                    if (state.threatAnalysis.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkCard.copy(alpha = 0.9f)),
                            border = BorderStroke(1.dp, Emerald500.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Analytics, null, tint = Emerald400, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("COMMUNITY THREAT ANALYSIS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Emerald400)
                                }
                                Spacer(Modifier.height(8.dp))
                                state.threatAnalysis.forEach { (type, count) ->
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(type.replace("_", " ").uppercase(), fontSize = 11.sp, color = TextPrimary)
                                        Text("$count Reports Near You", fontSize = 11.sp, fontWeight = FontWeight.Black, color = if (count > 3) Rose400 else Amber400)
                                    }
                                }
                            }
                        }
                    }

                    // Locate Me & Status
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkCard),
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                        border = BorderStroke(1.dp, DarkBorder)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    if (state.isGeocoding) {
                                        LinearProgressIndicator(modifier = Modifier.width(100.dp), color = Emerald400)
                                    } else {
                                        Text(state.address.ifEmpty { "Tapped location marked" }, style = MaterialTheme.typography.bodySmall, color = TextPrimary, maxLines = 1)
                                    }
                                    Text("Tap map to mark incident location", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                }
                                FloatingActionButton(
                                    onClick = { viewModel.useCurrentLocation() },
                                    modifier = Modifier.size(40.dp),
                                    containerColor = DarkInput,
                                    contentColor = Emerald400
                                ) {
                                    Icon(Icons.Default.MyLocation, null, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun inputColors() = OutlinedTextFieldDefaults.colors(
    unfocusedContainerColor = DarkInput,
    focusedContainerColor = DarkInput,
    unfocusedTextColor = TextPrimary,
    focusedTextColor = TextPrimary,
    unfocusedBorderColor = DarkBorder,
    focusedBorderColor = Emerald400,
    unfocusedLabelColor = TextMuted,
    focusedLabelColor = Emerald400
)
