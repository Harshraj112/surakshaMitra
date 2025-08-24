package com.example.sosapp.di

import android.content.Context
import com.example.sosapp.data.service.BluetoothService
import com.example.sosapp.data.service.EmergencyContactService
import com.example.sosapp.data.service.LocationService
import com.example.sosapp.data.service.NotificationService
import com.example.sosapp.data.service.VoskSpeechService
import com.example.sosapp.domain.repository.AuthRepository
import com.example.sosapp.domain.usecase.BluetoothUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ServiceModule {

    @Provides
    @Singleton
    fun provideBluetoothService(
        @ApplicationContext context: Context
    ): BluetoothService {
        return BluetoothService(context)
    }

    @Provides
    @Singleton
    fun provideBluetoothUseCase(
        bluetoothService: BluetoothService
    ): BluetoothUseCase {
        return BluetoothUseCase(bluetoothService)
    }

    @Provides
    @Singleton
    fun provideEmergencyContactService(
        authRepository: AuthRepository
    ): EmergencyContactService {
        return EmergencyContactService()
    }

    @Provides
    @Singleton
    fun provideLocationService(
        @ApplicationContext context: Context
    ): LocationService {
        return LocationService(context)
    }

    @Provides
    @Singleton
    fun provideNotificationService(
        @ApplicationContext context: Context
    ): NotificationService {
        return NotificationService(context)
    }

    @Provides
    @Singleton
    fun provideVoskSpeechService(): VoskSpeechService {
        return VoskSpeechService()
    }
}