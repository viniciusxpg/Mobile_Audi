package com.mobileaudi.player

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.mobileaudi.player.ui.MobileAudiApp
import com.mobileaudi.player.ui.theme.MobileAudiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MobileAudiTheme {
                MobileAudiApp()
            }
        }
    }
}
