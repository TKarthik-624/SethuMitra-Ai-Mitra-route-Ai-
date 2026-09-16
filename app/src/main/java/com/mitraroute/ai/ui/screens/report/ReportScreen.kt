package com.mitraroute.ai.ui.screens.report

import android.view.MotionEvent
import android.view.ViewGroup
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.activity.compose.BackHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mitraroute.ai.R
import com.mitraroute.ai.ui.theme.*
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay

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

    val incidentTypes = listOf(
        "road_damage", "flood", "landslide", "bridge_failure",
        "weather_blockage", "vehicle_breakdown", "other"
    )
    val incidentLabels = listOf(
        "🚧 Road Damage", "🌊 Flood", "⛰ Landslide",
        "🌉 Bridge Failure", "🌫 Weather Blockage",
        "🚛 Vehicle Breakdown", "⚠️ Other"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("REPORT INCIDENT", fontWeight = FontWeight.Black) },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.report_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, DarkBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {

                    // Incident Type dropdown
                    Text(stringResource(R.string.report_type), style = MaterialTheme.typography.labelLarge, color = TextMuted)
                    Spacer(modifier = Modifier.height(4.dp))
                    var typeExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = typeExpanded, onExpandedChange = { typeExpanded = it }) {
                        OutlinedTextField(
                            value = incidentLabels[selectedTypeIndex],
                            onValueChange = {},
                            readOnly = true,
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

                    Spacer(modifier = Modifier.height(14.dp))

                    // Severity picker
                    Text(stringResource(R.string.report_severity), style = MaterialTheme.typography.labelLarge, color = TextMuted)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        (1..5).forEach { sev ->
                            Button(
                                onClick = { viewModel.updateSeverity(sev) },
                                modifier = Modifier.size(48.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (state.severity == sev)
                                        when {
                                            sev <= 2 -> Amber500
                                            sev <= 3 -> Amber500
                                            else -> Rose500
                                        }
                                    else DarkInput
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("$sev", color = if (state.severity == sev) DarkBg else TextPrimary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Description
                    Text(stringResource(R.string.report_description), style = MaterialTheme.typography.labelLarge, color = TextMuted)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = state.description,
                        onValueChange = { viewModel.updateDescription(it) },
                        placeholder = { Text("Describe the incident...", color = TextMuted) },
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        colors = inputColors()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Map to set location
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.report_map_hint), style = MaterialTheme.typography.labelLarge, color = TextMuted)
                        TextButton(onClick = { viewModel.useCurrentLocation() }) {
                            Icon(Icons.Default.MyLocation, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Use My Location", fontSize = 12.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(12.dp))) {
                        AndroidView(
                            factory = {
                                MapView(context).apply {
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT, 200
                                    )
                                    setTileSource(TileSourceFactory.MAPNIK)
                                    setMultiTouchControls(true) // Enable zoom/pan
                                    controller.setZoom(12.0)
                                    controller.setCenter(GeoPoint(state.lat, state.lon))

                                    val tapMarker = Marker(this).apply {
                                        position = GeoPoint(state.lat, state.lon)
                                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                                        title = "Incident Location"
                                    }
                                    overlays.add(tapMarker)

                                    // Add single tap listener
                                    val overlay = object : Overlay() {
                                        override fun onSingleTapConfirmed(e: MotionEvent?, mapView: MapView): Boolean {
                                            val point = mapView.projection.fromPixels(
                                                e?.x?.toInt() ?: 0,
                                                e?.y?.toInt() ?: 0
                                            ) as GeoPoint
                                            viewModel.updateLocation(point.latitude, point.longitude)
                                            tapMarker.position = point
                                            mapView.invalidate()
                                            return true
                                        }
                                    }
                                    overlays.add(overlay)
                                }
                            },
                            update = { mapView ->
                                val point = GeoPoint(state.lat, state.lon)
                                mapView.controller.animateTo(point)
                                (mapView.overlays.find { it is Marker } as? Marker)?.position = point
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(modifier = Modifier.align(Alignment.TopStart).padding(8.dp).background(DarkBg.copy(alpha = 0.7f), RoundedCornerShape(4.dp)).padding(4.dp)) {
                            Text("Tap map to mark location", fontSize = 10.sp, color = TextPrimary)
                        }
                    }
                    if (state.address.isNotEmpty()) {
                        Text(
                            text = "📍 ${state.address}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Emerald400,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    Text(
                        text = "Lat: ${"%.4f".format(state.lat)}  Lon: ${"%.4f".format(state.lon)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Route selector
                    Text(stringResource(R.string.report_route), style = MaterialTheme.typography.labelLarge, color = TextMuted)
                    Spacer(modifier = Modifier.height(4.dp))
                    var routeExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = routeExpanded, onExpandedChange = { routeExpanded = it }) {
                        OutlinedTextField(
                            value = state.routes.find { it.id == state.routeId }?.name ?: "None",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(routeExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            colors = inputColors()
                        )
                        ExposedDropdownMenu(expanded = routeExpanded, onDismissRequest = { routeExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text("None") },
                                onClick = { viewModel.updateRoute(null); routeExpanded = false }
                            )
                            state.routes.forEach { route ->
                                DropdownMenuItem(
                                    text = { Text("${route.name} (${route.distance_km}km)") },
                                    onClick = { viewModel.updateRoute(route.id); routeExpanded = false }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Submit button
                    Button(
                        onClick = { viewModel.submitReport() },
                        enabled = !state.isSubmitting,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald500)
                    ) {
                        if (state.isSubmitting) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = TextPrimary, strokeWidth = 2.dp)
                        } else {
                            Text("📤 ${stringResource(R.string.report_btn)}")
                        }
                    }

                    // Result message
                    state.resultMessage?.let { msg ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (state.resultSuccess) Emerald500.copy(alpha = 0.15f)
                                else Rose500.copy(alpha = 0.15f)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (state.resultSuccess) "✅ $msg" else "❌ $msg",
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (state.resultSuccess) Emerald400 else Rose400
                            )
                        }
                    }
                }
            }

            // LIST OF SUBMITTED REPORTS
            if (state.submittedIncidents.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Text("YOUR RECENT REPORTS", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Emerald400)
                Spacer(modifier = Modifier.height(8.dp))
                state.submittedIncidents.take(5).forEach { incident ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkInput),
                        border = BorderStroke(1.dp, DarkBorder)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = when(incident.incident_type) {
                                    "road_damage" -> "🚧"
                                    "flood" -> "🌊"
                                    "landslide" -> "⛰"
                                    else -> "⚠️"
                                },
                                fontSize = 20.sp
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(incident.incident_type.replace("_", " ").uppercase(), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text(incident.description ?: "Verified report", fontSize = 11.sp, color = TextMuted)
                            }
                            Spacer(Modifier.weight(1f))
                            if (incident.status == "pending_sync") {
                                Icon(Icons.Default.CloudSync, null, tint = Amber400, modifier = Modifier.size(16.dp))
                            } else {
                                Icon(Icons.Default.CheckCircle, null, tint = Emerald400, modifier = Modifier.size(16.dp))
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
)
