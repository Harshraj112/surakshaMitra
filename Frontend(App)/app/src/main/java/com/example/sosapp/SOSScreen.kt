package com.example.sosapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.example.sosapp.ui.navigation.AppNavHost
import com.example.sosapp.ui.navigation.MainScaffold
import com.example.sosapp.ui.viewmodel.AuthViewModel

@Composable
fun SOSApp() {
    val authViewModel: AuthViewModel = hiltViewModel()
    val currentUser by authViewModel.currentUser.collectAsStateWithLifecycle()
    var isInitialized by remember { mutableStateOf(false) }
    val navController = rememberNavController()

    LaunchedEffect(currentUser) {
        if (!isInitialized) {
            // Give a small delay to ensure auth state is properly loaded
            kotlinx.coroutines.delay(100)
            isInitialized = true
        }
    }

    if (!isInitialized) {
        // Show loading screen while determining auth state
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary
            )
        }
        return
    }

    MainScaffold(navController = navController) { paddingValues ->
        AppNavHost(
            navController = navController,
            isLoggedIn = currentUser != null,
            onLoginSuccess = {
            },
            modifier = Modifier.padding(paddingValues),
            onLogout = {
            }
        )
    }
}