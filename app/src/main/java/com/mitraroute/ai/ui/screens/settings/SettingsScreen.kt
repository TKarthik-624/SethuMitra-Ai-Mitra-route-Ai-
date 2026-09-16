package com.mitraroute.ai.ui.screens.settings

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mitraroute.ai.R
import com.mitraroute.ai.ui.theme.*
import com.mitraroute.ai.util.PrefsManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val prefs = remember { PrefsManager(context) }
    val scope = rememberCoroutineScope()
    
    var currentLang by remember { mutableStateOf(prefs.getLanguage()) }
    var currentUnits by remember { mutableStateOf(prefs.getUnits()) }
    var notificationsEnabled by remember { mutableStateOf(prefs.getNotifications()) }
    var voiceEnabled by remember { mutableStateOf(prefs.getVoice()) }

    val languages = listOf(
        "en" to "English",
        "hi" to "हिन्दी (Hindi)",
        "te" to "తెలుగు (Telugu)",
        "ta" to "தமிழ் (Tamil)",
        "kn" to "ಕನ್ನಡ (Kannada)",
        "ml" to "മലയാളം (Malayalam)",
        "mr" to "मराठी (Marathi)",
        "bn" to "বাংলা (Bengali)",
        "gu" to "ગુજરાતી (Gujarati)",
        "pa" to "ਪੰਜਾਬੀ (Punjabi)",
        "or" to "ଓଡ଼ିଆ (Odia)",
        "as" to "অসমীয়া (Assamese)",
        "ur" to "اردو (Urdu)",
        "ne" to "नेपाली (Nepali)"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = Emerald400
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Language Selection
        SettingsSection(title = stringResource(R.string.settings_language)) {
            var expanded by remember { mutableStateOf(false) }
            Box {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = true },
                    color = DarkCard,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, DarkBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(languages.find { it.first == currentLang }?.second ?: "English", color = TextPrimary)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Emerald400)
                    }
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(DarkCard)
                ) {
                    languages.forEach { (code, label) ->
                        DropdownMenuItem(
                            text = { Text(label, color = TextPrimary) },
                            onClick = {
                                currentLang = code
                                expanded = false
                                scope.launch {
                                    prefs.setLanguage(code)
                                    // Trigger activity recreation to apply new locale
                                    (context as? Activity)?.recreate()
                                }
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Units Selection
        SettingsSection(title = stringResource(R.string.settings_units)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkCard)
                    .padding(4.dp)
            ) {
                UnitButton(
                    label = stringResource(R.string.settings_km),
                    selected = currentUnits == "km",
                    onClick = {
                        currentUnits = "km"
                        scope.launch { prefs.setUnits("km") }
                    },
                    modifier = Modifier.weight(1f)
                )
                UnitButton(
                    label = stringResource(R.string.settings_miles),
                    selected = currentUnits == "miles",
                    onClick = {
                        currentUnits = "miles"
                        scope.launch { prefs.setUnits("miles") }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Notifications Toggle
        SettingsToggle(
            label = stringResource(R.string.settings_notifications),
            checked = notificationsEnabled,
            onCheckedChange = {
                notificationsEnabled = it
                scope.launch { prefs.setNotifications(it) }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Voice Toggle
        SettingsToggle(
            label = stringResource(R.string.settings_voice),
            checked = voiceEnabled,
            onCheckedChange = {
                voiceEnabled = it
                scope.launch { prefs.setVoice(it) }
            }
        )
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        content()
    }
}

@Composable
private fun UnitButton(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    Surface(
        modifier = modifier
            .height(40.dp)
            .clickable { onClick() },
        color = if (selected) Emerald500 else Color.Transparent,
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                color = if (selected) Slate950 else TextMuted,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun SettingsToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = DarkCard,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = TextPrimary, fontWeight = FontWeight.Medium)
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Emerald400,
                    checkedTrackColor = Emerald600.copy(alpha = 0.5f),
                    uncheckedThumbColor = TextMuted,
                    uncheckedTrackColor = DarkInput
                )
            )
        }
    }
}
