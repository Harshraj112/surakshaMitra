package com.example.sosapp.data.local.cache

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.sosapp.data.model.UserData
import com.google.gson.Gson
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UserCacheImplInstrumentedTest {

    private lateinit var userCache: UserCacheImpl
    private lateinit var context: Context
    private val gson = Gson()

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        userCache = UserCacheImpl(context, gson)
    }

    @Test
    fun saveAndGetUser_returnsSameUser() = runTest {
        val user = UserData(
            id = "123",
            fullName = "John Doe",
            email = "john@example.com"
        )

        userCache.saveUser(user)
        val retrieved = userCache.getUser()

        assertNotNull(retrieved)
        assertEquals(user.id, retrieved?.id)
        assertEquals(user.fullName, retrieved?.fullName)
        assertEquals(user.email, retrieved?.email)
    }

    @Test
    fun clearUser_removesUserData() = runTest {
        val user = UserData("123", "John Doe", "john@example.com")
        userCache.saveUser(user)

        userCache.clearUser()
        val retrieved = userCache.getUser()

        assertNull(retrieved)
    }

    @Test
    fun getUser_whenNoData_returnsNull() = runTest {
        val retrieved = userCache.getUser()
        assertNull(retrieved)
    }
}
