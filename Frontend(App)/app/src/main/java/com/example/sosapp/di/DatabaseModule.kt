package com.example.sosapp.di

import android.content.Context
import androidx.room.Room
import com.example.sosapp.data.local.cache.UserCache
import com.example.sosapp.data.local.cache.UserCacheImpl
import com.example.sosapp.data.local.db.AppDatabase
import com.example.sosapp.data.local.db.UserDao
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "sos_database"
        ).build()
    }

    @Provides
    fun provideUserDao(database: AppDatabase): UserDao {
        return database.userDao()
    }

    @Provides
    @Singleton
    fun provideUserCache(@ApplicationContext context: Context, gson: Gson): UserCache {
        return UserCacheImpl(context, gson)
    }
}