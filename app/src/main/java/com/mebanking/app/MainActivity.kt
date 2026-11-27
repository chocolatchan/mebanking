package com.mebanking.app

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.mebanking.app.ui.MEbankingApp
import com.mebanking.app.ui.theme.MEbankingTheme

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Prevent non-system overlay windows (tapjacking/screen-overlay fraud protection).
            window.setHideOverlayWindows(true)
        }
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
