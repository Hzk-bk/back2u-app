package com.hzk.back2u

import com.hzk.back2u.data.AuthRepository
import com.hzk.back2u.data.FirebaseAuthRepository
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.hzk.back2u.ui.navigation.AppNavHost
import com.hzk.back2u.ui.theme.Back2UTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val authRepository: AuthRepository = FirebaseAuthRepository()
        setContent {
            Back2UTheme {
                AppNavHost(authRepository)
            }
        }
    }
}