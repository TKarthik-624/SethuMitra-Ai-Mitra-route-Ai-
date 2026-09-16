package com.mitraroute.ai.ui.screens.command

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mitraroute.ai.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommandDashboardScreen(
    viewModel: CommandDashboardViewModel = viewModel(),
    onNavigate: (String) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            viewModel.refreshLocation()
        }
    }

    LaunchedEffect(Unit) {
        val fineGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fineGranted && !coarseGranted) {
            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        } else {
            viewModel.refreshLocation()
        }
    }

    DisposableEffect(Unit) {
        onDispose { /* viewModel handles stopTracking in onCleared */ }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "MITRAROUTE",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = Emerald400
                    )
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshLocation() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Emerald400)
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            
            // 1. CURRENT LOCATION
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, DarkBorder)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(color = Emerald400, modifier = Modifier.size(24.dp))
                    } else {
                        Icon(Icons.Default.MyLocation, null, tint = Emerald400, modifier = Modifier.size(32.dp))
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("CURRENT LOCATION", style = MaterialTheme.typography.labelSmall, color = TextMuted, fontWeight = FontWeight.Bold)
                    Text(state.locationName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = TextPrimary, textAlign = TextAlign.Center)
                    Text("${state.area}${if(state.area.isNotEmpty()) ", " else ""}${state.country}", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                }
            }

            // 2. WEATHER
            state.currentWeather?.let { weather ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, DarkBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("WEATHER", style = MaterialTheme.typography.labelSmall, color = TextMuted, fontWeight = FontWeight.Bold)
                            val temp = weather.current?.temperature_2m?.toInt() ?: "--"
                            val cond = when(weather.current?.weather_code) {
                                0 -> "Clear"
                                1,2,3 -> "Partly Cloudy"
                                61,63,65 -> "Rainy"
                                else -> "Cloudy"
                            }
                            Text("$temp°C", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = Amber400)
                            Text(cond, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                        }
                        Icon(Icons.Default.Cloud, null, tint = Emerald400, modifier = Modifier.size(48.dp))
                    }
                }
            }

            // 3. LOCAL SAFETY / CONDITIONS
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, DarkBorder)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("LOCAL SAFETY / CONDITIONS", style = MaterialTheme.typography.labelSmall, color = Emerald400, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    if (state.activeIncidents.isEmpty()) {
                        Text("No verified incident data available for this location.", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    } else {
                        state.activeIncidents.take(2).forEach { incident ->
                            Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(bottom = 8.dp)) {
                                Icon(Icons.Default.Warning, null, tint = Rose400, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(incident.incident_type.uppercase(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    Text(incident.description ?: "Verified report.", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                                }
                            }
                        }
                    }
                }
            }

            // 4. LARGE BUTTONS
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                BigButton(Icons.Default.Route, "ROUTE PLANNER", Emerald500) { onNavigate("planner") }
                BigButton(Icons.Default.LocalHospital, "NEARBY HOSPITALS", Amber500) { onNavigate("hospitals") }
                BigButton(Icons.Default.Cloud, "WEATHER", Color(0xFF60A5FA)) { onNavigate("weather") }
                BigButton(Icons.Default.Warning, "REPORT INCIDENT", Rose400) { onNavigate("report") }
            }
        }
    }
}

@Composable
private fun BigButton(icon: ImageVector, label: String, color: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(64.dp),
        colors = ButtonDefaults.buttonColors(containerColor = DarkCard),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(16.dp))
            Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, null, tint = TextMuted)
        }
    }
}
