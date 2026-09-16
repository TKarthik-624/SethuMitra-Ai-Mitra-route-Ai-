# MitraRoute AI — Android App 🗺️

**AI-Based Smart Logistics & Accessibility Intelligence Platform for North Eastern Region (NER)**

Built for SIH 2026 Problem Statement 26002 — Team SetuMitra AI

## Features

- **🗺️ Route Map** — Interactive osmdroid map with all NER routes, risk-colored polylines, and incident markers
- **🤖 AI Risk Engine** — Tap any route to get real-time risk scoring (weather + incidents + terrain)
- **📋 Incident Reporting** — Report road damage, floods, landslides with severity and GPS coordinates
- **📡 Offline Support** — Incidents queue locally and sync when network returns (WorkManager)
- **🌤️ Weather Dashboard** — Live weather data for 6 NER cities
- **📊 Dashboard** — Stats overview with active incidents and sync status
- **🌐 Multi-Language** — English, Hindi, Assamese, Bengali, Manipuri
- **🔐 RBAC** — Role-based access (Admin, District Officer, Field Agent)

## Tech Stack

| Layer | Technology |
|-------|-----------|
| UI | Kotlin + Jetpack Compose + Material 3 |
| Map | osmdroid (OpenStreetMap) |
| Networking | Retrofit + OkHttp + Kotlinx Serialization |
| Database | Room (SQLite) |
| Offline Sync | WorkManager |
| State | ViewModel + StateFlow |
| Preferences | DataStore |

## Setup

### 1. Start the Backend Server

The Android app needs the MitraRoute API server running.

```bash
cd ../mitraroute  # from the backend directory
pip install -r requirements.txt
uvicorn backend.main:app --host 0.0.0.0 --port 8000
```

### 2. Open in Android Studio

1. Open Android Studio
2. File → Open → Select `MitraRouteApp/` folder
3. Wait for Gradle sync to complete
4. If prompted about SDK versions, install the required SDK (API 34)

### 3. Configure Server URL

Edit `app/build.gradle.kts` and change `API_BASE_URL`:

```kotlin
// For Android Emulator (uses host machine's localhost)
buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8000/\"")

// For physical device on same WiFi
buildConfigField("String", "API_BASE_URL", "\"http://YOUR_PC_IP:8000/\"")
```

### 4. Run

- Select a device/emulator
- Click ▶️ Run

## Demo Accounts

| Username | Password | Role |
|----------|----------|------|
| admin | admin123 | Administrator |
| field_agent1 | demo123 | Field Agent (Assam) |
| field_agent2 | demo123 | Field Agent (Meghalaya) |
| officer1 | demo123 | District Officer |

## Project Structure

```
MitraRouteApp/
├── app/src/main/
│   ├── AndroidManifest.xml
│   ├── java/com/mitraroute/ai/
│   │   ├── MainActivity.kt
│   │   ├── MitraRouteApp.kt          # Application (osmdroid + WorkManager init)
│   │   ├── data/
│   │   │   ├── api/                   # Retrofit API service
│   │   │   ├── model/                 # Data classes (Route, Incident, Weather, etc.)
│   │   │   ├── local/                 # Room database + DAOs
│   │   │   └── repository/            # Repository (API + cache + offline queue)
│   │   ├── ui/
│   │   │   ├── theme/                 # Dark theme (Color, Type, Theme)
│   │   │   ├── navigation/            # Bottom nav with 4 tabs
│   │   │   ├── screens/
│   │   │   │   ├── login/             # Login screen + ViewModel
│   │   │   │   ├── map/               # osmdroid map + risk panel
│   │   │   │   ├── report/            # Incident form + map click
│   │   │   │   ├── weather/           # Weather grid
│   │   │   │   └── dashboard/         # Stats + incident list
│   │   │   └── components/            # RiskBadge, IncidentCard, WeatherCard
│   │   └── util/
│   │       ├── PrefsManager.kt        # DataStore preferences
│   │       └── OfflineSyncWorker.kt   # WorkManager background sync
│   └── res/
│       ├── values/                    # English strings
│       ├── values-hi/                 # Hindi
│       ├── values-as/                 # Assamese
│       ├── values-bn/                 # Bengali
│       └── values-mn/                 # Manipuri
├── build.gradle.kts                   # Project-level
├── app/build.gradle.kts               # App dependencies
└── gradle/libs.versions.toml          # Version catalog
```

## Architecture

```
┌─────────────────────────────────────────┐
│         Jetpack Compose UI              │
│  Map │ Report │ Weather │ Dashboard     │
└──────────────┬──────────────────────────┘
               │ StateFlow
┌──────────────▼──────────────────────────┐
│            ViewModels                   │
│  MapVM │ ReportVM │ WeatherVM │ DashVM  │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│          Repository Layer               │
│  API calls → Room cache → Offline queue │
└──────────┬──────────┬───────────────────┘
           │          │
    ┌──────▼──┐  ┌────▼─────┐
    │ Retrofit│  │   Room   │
    │  (API)  │  │  (Cache) │
    └────┬────┘  └──────────┘
         │
┌────────▼────────────────────────────────┐
│   FastAPI Backend (mitraroute/)          │
│   AI Risk Engine │ Weather │ RBAC       │
└─────────────────────────────────────────┘
```
