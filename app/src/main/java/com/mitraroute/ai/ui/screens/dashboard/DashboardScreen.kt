package com.mitraroute.ai.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mitraroute.ai.R
import com.mitraroute.ai.ui.components.IncidentCard
import com.mitraroute.ai.ui.theme.*

@Composable
fun DashboardScreen(viewModel: DashboardViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "📊 Logistics Insights",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black
            )
            TextButton(onClick = { viewModel.loadData() }) {
                Text("Sync Intelligence", color = Emerald400)
            }
        }

        // Stats grid
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                value = "${state.stats.total_routes}",
                label = stringResource(R.string.dashboard_total_routes),
                color = Emerald400,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                value = "${state.stats.active_incidents}",
                label = stringResource(R.string.dashboard_active_incidents),
                color = Amber400,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                value = "${state.stats.critical_routes}",
                label = stringResource(R.string.dashboard_critical_routes),
                color = Rose400,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                value = "${state.stats.total_users}",
                label = stringResource(R.string.dashboard_total_users),
                color = Emerald400,
                modifier = Modifier.weight(1f)
            )
        }

        // Pending sync indicator
        if (state.pendingSync > 0) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Amber500.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "\uD83D\uDD04 ${state.pendingSync} reports pending sync",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Amber400
                )
            }
        }

        // Incident list
        Text(
            text = "\uD83D\uDCCB ${stringResource(R.string.dashboard_active_incidents)}",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )

        if (state.incidents.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("\u2705", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(stringResource(R.string.dashboard_no_incidents), color = TextMuted)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkCard)
            ) {
                items(state.incidents.take(20)) { incident ->
                    IncidentCard(incident = incident)
                }
            }
        }
    }
}

@Composable
private fun StatCard(value: String, label: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}
