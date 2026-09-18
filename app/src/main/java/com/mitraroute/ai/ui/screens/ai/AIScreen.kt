package com.mitraroute.ai.ui.screens.ai

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mitraroute.ai.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIScreen(
    viewModel: AIViewModel = viewModel(),
    onBack: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        // AI synthesis based on structured data if available
        viewModel.generateIntelligence(
            originName = "Current Location", 
            destName = "Target Destination", 
            distanceKm = "Searching...", 
            duration = "--", 
            weather = "Analyzing...", 
            riskLevel = "LOW", 
            incidents = emptyList()
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI INTELLIGENCE", fontWeight = FontWeight.Black) },
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(top = 100.dp), color = Emerald400)
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Emerald500.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Amber400)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "LOGISTICS ANALYSIS",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Amber400
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = state.summary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                            lineHeight = 20.sp
                        )
                    }
                }
                
                if (state.transitOptions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Text("LIVE TRANSIT DETAILS", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Emerald400)
                    Spacer(modifier = Modifier.height(8.dp))
                    state.transitOptions.forEach { opt ->
                        TransitCard(opt)
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Verify critical conditions with local authorities.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun TransitCard(opt: TransitOption) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        colors = CardDefaults.cardColors(containerColor = DarkInput),
        border = BorderStroke(1.dp, DarkBorder)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(if (opt.type == "BUS") "🚌" else "🚆", fontSize = 20.sp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("${opt.type}: ${opt.serviceName}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Dep: ${opt.departureTime} • Arr: ${opt.arrivalTime}", fontSize = 12.sp, color = TextMuted)
                Text("${opt.stops} stops • ${opt.transfers} transfers", fontSize = 11.sp, color = TextMuted)
            }
            if (opt.fare != null) {
                Text(opt.fare, fontWeight = FontWeight.Black, color = Amber400, fontSize = 14.sp)
            }
        }
    }
}
