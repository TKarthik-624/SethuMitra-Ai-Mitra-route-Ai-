package com.mitraroute.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.mitraroute.ai.ui.navigation.AppNavigation
import com.mitraroute.ai.ui.theme.MitraRouteTheme
import com.mitraroute.ai.util.PrefsManager
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val prefs = PrefsManager(this)
        
        setContent {
            val language by prefs.getLanguageFlow().collectAsState(initial = "en")
            
            // Set Locale
            val locale = Locale(language)
            Locale.setDefault(locale)
            val resources = this.resources
            val configuration = resources.configuration
            configuration.setLocale(locale)
            resources.updateConfiguration(configuration, resources.displayMetrics)

            MitraRouteTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}
