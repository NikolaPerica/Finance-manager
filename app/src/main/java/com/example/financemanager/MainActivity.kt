package com.example.financemanager

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.example.financemanager.ui.navigation.FinanceNavHost
import com.example.financemanager.ui.theme.FinanceTheme

// FragmentActivity rather than ComponentActivity because BiometricPrompt needs one.
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            FinanceTheme {
                FinanceNavHost()
            }
        }
    }
}
