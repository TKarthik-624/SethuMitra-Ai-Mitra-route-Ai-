package com.mitraroute.ai.ui.screens.weather

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mitraroute.ai.ui.components.WeatherCard
import com.mitraroute.ai.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(
    viewModel: WeatherViewModel = viewModel(),
    onBack: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    
    BackHandler { onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("WEATHER", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadWeather() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Emerald400)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg)
            )
        },
        containerColor = DarkBg
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Emerald400)
            } else if (state.error != null) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(state.error!!, color = Rose400, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { viewModel.loadWeather() }, colors = ButtonDefaults.buttonColors(containerColor = DarkInput)) {
                        Text("RETRY")
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    item {
                        state.weatherData?.let { weather ->
                            WeatherCard(weather = weather)
                        }
                    }

                    item {
                        Text("5-DAY FORECAST", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = TextMuted)
                        Spacer(Modifier.height(12.dp))
                        state.weatherData?.daily?.let { daily ->
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                daily.time.forEachIndexed { index, date ->
                                    ForecastItem(
                                        date = date,
                                        tempMax = daily.temperature_2m_max[index],
                                        tempMin = daily.temperature_2m_min[index],
                                        conditionCode = daily.weather_code[index]
                                    )
                                }
                            }
                        }
                    }
                    
                    item { Spacer(Modifier.height(32.dp)) }
                }
            }
        }
    }
}

@Composable
private fun ForecastItem(date: String, tempMax: Double, tempMin: Double, conditionCode: Int) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* Detail if needed */ },
        color = DarkCard,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, DarkBorder)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(date, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                Text(getWeatherCondition(conditionCode), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${tempMax.toInt()}°", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Amber400)
                Spacer(Modifier.width(8.dp))
                Text("${tempMin.toInt()}°", fontSize = 18.sp, fontWeight = FontWeight.Black, color = TextMuted)
            }
        }
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
