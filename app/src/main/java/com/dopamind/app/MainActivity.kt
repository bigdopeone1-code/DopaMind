package com.dopamind.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import com.dopamind.app.core.di.LocalAppContainer
import com.dopamind.app.core.navigation.DopaMindNavHost
import com.dopamind.app.core.theme.BackgroundPrimary
import com.dopamind.app.core.theme.DopaMindTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as DopaMindApplication).container

        setContent {
            CompositionLocalProvider(LocalAppContainer provides container) {
                DopaMindTheme {
                    Surface(modifier = Modifier.fillMaxSize(), color = BackgroundPrimary) {
                        DopaMindNavHost()
                    }
                }
            }
        }
    }
}
