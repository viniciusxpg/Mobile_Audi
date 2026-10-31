package io.github.viniciusxpg.vinil

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.viniciusxpg.vinil.ui.VinilApp
import io.github.viniciusxpg.vinil.ui.theme.VinilTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VinilTheme {
                VinilApp()
            }
        }
    }
}
