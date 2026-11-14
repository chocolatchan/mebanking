package com.mebanking.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.mebanking.app.ui.MEbankingApp
import com.mebanking.app.ui.theme.MEbankingTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            MEbankingTheme {
                MEbankingApp()
            }
        }
    }
}
