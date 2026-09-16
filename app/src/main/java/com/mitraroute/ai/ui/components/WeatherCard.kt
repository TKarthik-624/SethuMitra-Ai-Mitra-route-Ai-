package com.mitraroute.ai.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WindPower
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mitraroute.ai.data.model.OpenMeteoResponse
import com.mitraroute.ai.ui.theme.*

@Composable
fun WeatherCard(weather: OpenMeteoResponse, modifier: Modifier = Modifier) {
    val current = weather.current ?: return
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, DarkBorder)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "CURRENT WEATHER",
                        style = MaterialTheme.typography.labelMedium,
                        color = Emerald400,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = getWeatherCondition(current.weather_code),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                }
                Text(
                    text = "${current.temperature_2m.toInt()}°C",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Black,
                    color = Amber400
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = DarkBorder)
            Spacer(modifier = Modifier.height(20.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                WeatherStat(Icons.Default.WaterDrop, "HUMIDITY", "${current.relative_humidity_2m ?: "--"}%")
                WeatherStat(Icons.Default.WindPower, "WIND", "${current.wind_speed_10m} km/h")
                WeatherStat(Icons.Default.Thermostat, "FEELS", "${current.apparent_temperature?.toInt() ?: "--"}°C")
            }
        }
    }
}

@Composable
private fun WeatherStat(icon: ImageVector, label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = TextMuted, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted, fontWeight = FontWeight.Bold)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Black, color = TextPrimary)
    }
}

private fun getWeatherCondition(code: Int): String = when (code) {
    0 -> "Clear Sky"
    1, 2, 3 -> "Partly Cloudy"
    45, 48 -> "Foggy"
    51, 53, 55 -> "Drizzle"
    61, 63, 65 -> "Rainy"
    71, 73, 75 -> "Snowy"
    80, 81, 82 -> "Rain Showers"
    95, 96, 99 -> "Thunderstorm"
    else -> "Cloudy"
}
