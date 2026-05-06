package com.zeus.lineagent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.zeus.lineagent.ui.AppNavigation
import com.zeus.lineagent.ui.theme.ZeusTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as ZeusApp).container
        setContent {
            ZeusTheme {
                AppNavigation(container = container)
            }
        }
    }
}
