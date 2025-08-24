package com.example.sosapp.domain.usecase

import com.example.sosapp.data.model.AuthResult
import com.example.sosapp.data.service.LocationService
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class LocationUseCase @Inject constructor(
    private val locationService: LocationService
) {
    suspend fun getCurrentLocation(): AuthResult<Pair<Double, Double>> {
        return locationService.getCurrentLocation()
    }

    fun getLocationUpdates(): Flow<Pair<Double, Double>> {
        return locationService.getLocationUpdates()
    }
}