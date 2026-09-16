package com.mitraroute.ai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mitraroute.ai.R
import com.mitraroute.ai.ui.theme.*

@Composable
fun RiskBadge(riskLevel: String?, modifier: Modifier = Modifier) {
    val color = riskColor(riskLevel)
    val text = when (riskLevel) {
        "low" -> stringResource(R.string.risk_low)
        "medium" -> stringResource(R.string.risk_medium)
        "high" -> stringResource(R.string.risk_high)
        "critical" -> stringResource(R.string.risk_critical)
        else -> "—"
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.2f))
            .padding(horizontal = 12.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text.uppercase(),
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun SeverityDots(severity: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(5) { index ->
            val active = index < severity
            val color = when {
                !active -> DarkInput
                severity <= 2 -> Emerald400
                severity <= 3 -> Amber500
                else -> Rose500
            }
            Box(
                modifier = Modifier
                    .width(18.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color)
            )
        }
    }
}
