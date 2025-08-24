package com.example.sosapp.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.sosapp.data.model.UserData

@Dao
interface UserDao {
    @Query("SELECT * FROM user_data LIMIT 1")
    fun getUser(): UserData?

    @Insert
    fun insertUser(user: UserData)
}