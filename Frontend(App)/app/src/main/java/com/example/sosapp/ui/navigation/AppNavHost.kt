package com.example.sosapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.sosapp.ui.screen.auth.LoginScreen
import com.example.sosapp.ui.screen.auth.OTPVerificationScreen
import com.example.sosapp.ui.screen.emergency.EmergencyContactScreen
import com.example.sosapp.ui.screen.emergency.EmergencyDirectoryScreen
import com.example.sosapp.ui.screen.emergency.PanicScreen
import com.example.sosapp.ui.screen.profile.ProfileScreen
import com.example.sosapp.ui.viewmodel.AuthViewModel

@Composable
fun AppNavHost(
    navController: NavHostController,
    isLoggedIn: Boolean,
    onLoginSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    onLogout: () -> Unit = {}
) {
    val authViewModel: AuthViewModel = hiltViewModel()

    LaunchedEffect(isLoggedIn) {
        val currentRoute = navController.currentDestination?.route

        if (isLoggedIn && (currentRoute == NavRoutes.Login.route || currentRoute == NavRoutes.OTPVerification.route)) {
            navController.navigate(NavRoutes.Profile.route) {
                popUpTo(0) { inclusive = true }
            }
        } else if (!isLoggedIn && currentRoute != NavRoutes.Login.route && currentRoute != NavRoutes.OTPVerification.route) {
            navController.navigate(NavRoutes.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = if (isLoggedIn) NavRoutes.Profile.route else NavRoutes.Login.route,
        modifier = modifier
    ) {
        composable(NavRoutes.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    onLoginSuccess()
                },
                onNavigateToOTP = {
                    navController.navigate(NavRoutes.OTPVerification.route)
                },
                viewModel = authViewModel
            )
        }

        composable(NavRoutes.OTPVerification.route) {
            OTPVerificationScreen(
                viewModel = authViewModel,
                onOTPVerified = {
                    onLoginSuccess()
                },
                onBackToRegister = {
                    navController.popBackStack()
                }
            )
        }

        composable(NavRoutes.Panic.route) {
            PanicScreen()
        }

        composable(NavRoutes.EmergencyContacts.route) {
            EmergencyContactScreen()
        }

        composable(NavRoutes.EmergencyDirectory.route) {
            EmergencyDirectoryScreen()
        }

        composable(NavRoutes.Profile.route) {
            ProfileScreen(
                onEditProfile = {
                    navController.navigate(NavRoutes.EmergencyContacts.route)
                },
                onLogout = {
                    authViewModel.logout()
                    onLogout()
                    // Navigation will be handled by LaunchedEffect above
                },
                viewModel = authViewModel
            )
        }
    }
}