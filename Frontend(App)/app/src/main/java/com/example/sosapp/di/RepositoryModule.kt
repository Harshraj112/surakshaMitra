package com.example.sosapp.di

import com.example.sosapp.data.local.cache.UserCache
import com.example.sosapp.data.local.preferences.TokenManager
import com.example.sosapp.data.remote.api.AuthApiService
import com.example.sosapp.data.repository.AuthRepositoryImpl
import com.example.sosapp.domain.repository.AuthRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    @Provides
    @Singleton
    fun provideAuthRepository(
        apiService: AuthApiService,
        tokenManager: TokenManager,
        userCache: UserCache
    ): AuthRepository {
        return AuthRepositoryImpl(
            api = apiService,
            tokenManager = tokenManager,
            userCache = userCache
        )
    }
}