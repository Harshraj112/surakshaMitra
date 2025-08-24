package com.example.sosapp.data.service

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import com.example.sosapp.data.model.AuthResult
import com.example.sosapp.domain.model.AppError
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Tasks
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class LocationService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): AuthResult<Pair<Double, Double>> {
        return try {
            if (!isLocationEnabled()) {
                return AuthResult.Error(AppError.LocationError("Location services are disabled"))
            }

            val location = suspendCancellableCoroutine { continuation ->
                try {
                    val task = fusedLocationClient.lastLocation
                    task.addOnSuccessListener { location ->
                        continuation.resume(location)
                    }
                    task.addOnFailureListener {
                        continuation.resume(null)
                    }
                } catch (e: SecurityException) {
                    continuation.resume(null)
                }
            }

            location?.let {
                AuthResult.Success(Pair(it.latitude, it.longitude))
            } ?: AuthResult.Error(AppError.LocationError("Unable to get current location"))

        } catch (e: SecurityException) {
            AuthResult.Error(AppError.PermissionError("Location permission not granted"))
        } catch (e: Exception) {
            AuthResult.Error(AppError.LocationError("Failed to get location: ${e.message}"))
        }
    }

    @SuppressLint("MissingPermission")
    fun getLocationUpdates(): Flow<Pair<Double, Double>> = flow {
        if (!isLocationEnabled()) {
            throw Exception("Location services are disabled")
        }

        // For now, just emit a single location
        // In a real implementation, you'd set up location callbacks
        val location = fusedLocationClient.lastLocation
        try {
            val result = Tasks.await(location)
            result?.let {
                emit(Pair(it.latitude, it.longitude))
            }
        } catch (e: Exception) {
            throw Exception("Failed to get location updates: ${e.message}")
        }
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }
}