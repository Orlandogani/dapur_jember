package com.leanecorps.dapurjember

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.leanecorps.dapurjember.core.designsystem.theme.DapurJemberTheme
import com.leanecorps.dapurjember.navigation.DapurJemberNavHost
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DapurJemberTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    DapurJemberNavHost(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}
