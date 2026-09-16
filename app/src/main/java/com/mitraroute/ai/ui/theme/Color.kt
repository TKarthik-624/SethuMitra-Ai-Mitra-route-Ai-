package com.mitraroute.ai.ui.theme

import androidx.compose.ui.graphics.Color

// Tactical Logistics Palette (Slate, Emerald, Amber)

// Slate - Backgrounds & UI Elements
val Slate950 = Color(0xFF020617)
val Slate900 = Color(0xFF0F172A)
val Slate800 = Color(0xFF1E293B)
val Slate700 = Color(0xFF334155)
val Slate600 = Color(0xFF475569)
val Slate500 = Color(0xFF64748B)
val Slate400 = Color(0xFF94A3B8)
val Slate300 = Color(0xFFCBD5E1)
val Slate100 = Color(0xFFF1F5F9)

// Emerald - Primary & Positive Action
val Emerald600 = Color(0xFF059669)
val Emerald500 = Color(0xFF10B981)
val Emerald400 = Color(0xFF34D399)
val Emerald300 = Color(0xFF6EE7B7)
val Emerald900 = Color(0xFF064E3B)

// Amber - Warning & Secondary Action
val Amber600 = Color(0xFFD97706)
val Amber500 = Color(0xFFF59E0B)
val Amber400 = Color(0xFFFBBF24)

// Danger / Risk
val Rose500 = Color(0xFFF43F5E)
val Rose400 = Color(0xFFFB7185)

// Mapping to current theme logic
val DarkBg = Slate950
val DarkCard = Slate900
val DarkInput = Slate800
val DarkBorder = Slate700
val TextPrimary = Slate100
val TextMuted = Slate400

val RiskLow = Emerald500
val RiskMedium = Amber500
val RiskHigh = Rose500
val RiskCritical = Color(0xFF7F1D1D)

fun riskColor(level: String?): Color = when (level) {
    "low" -> RiskLow
    "medium" -> RiskMedium
    "high" -> RiskHigh
    "critical" -> RiskCritical
    else -> Emerald400
}
