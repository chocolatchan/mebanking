package com.mebanking.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.mebanking.app.ui.MEbankingApp
import com.mebanking.app.ui.theme.MEbankingTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (!BuildConfig.DEBUG) {
            // Block screenshots and screen recording of real financial data in
            // release builds. Debug builds stay recordable for the course demo.
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE,
            )
        }
        setContent {
            MEbankingTheme {
                MEbankingApp()
            }
        }
    }
}
