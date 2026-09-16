package com.mitraroute.ai.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mitraroute.ai.data.model.Incident
import com.mitraroute.ai.ui.theme.*

private val INCIDENT_ICONS = mapOf(
    "road_damage" to "\uD83D\uDEA7",
    "flood" to "\uD83C\uDF0A",
    "landslide" to "\u26F0\uFE0F",
    "bridge_failure" to "\uD83C\uDF09",
    "weather_blockage" to "\uD83C\uDF2B\uFE0F",
    "vehicle_breakdown" to "\uD83D\uDE9B",
    "other" to "\u26A0\uFE0F",
)

@Composable
fun IncidentCard(
    incident: Incident,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = INCIDENT_ICONS[incident.incident_type] ?: "\u26A0\uFE0F",
                fontSize = 24.sp
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = incident.incident_type.replace("_", " ").uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                if (incident.description != null) {
                    Text(
                        text = incident.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
                Text(
                    text = "Severity: ${incident.severity}/5 \u00B7 ${incident.status}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
            SeverityDots(severity = incident.severity)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Divider(color = DarkBorder, thickness = 0.5.dp)
    }
}
