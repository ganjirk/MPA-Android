package com.grk.mpa.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.grk.mpa.app.ui.MpaApp
import com.grk.mpa.app.ui.theme.MPAAndroidTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { MPAAndroidTheme { MpaApp() } }
    }
}
