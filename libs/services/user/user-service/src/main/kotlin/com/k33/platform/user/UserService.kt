package com.k33.platform.user

import com.k33.platform.utils.analytics.Log
import io.firestore4k.typed.FirestoreClient
import io.firestore4k.typed.div
import java.util.*

object UserService {

    private val firestoreClient by lazy { FirestoreClient() }

    suspend fun UserId.createUser(
        email: String,
        webClientId: String,
        idProvider: String?,
    ): User? {
        suspend fun createUser(): User? {
            val analyticsId = UUID.randomUUID().toString()
            firestoreClient.put(
                users / this,
                User(
                    userId = value,
                    analyticsId = analyticsId
                )
            )
            UserEventHandler.onNewUserCreated(
                email = email,
                userAnalyticsId = analyticsId,
                webClientId = webClientId,
                idProvider = idProvider,
            )
            return fetchUser()
        }
        return fetchUser()
            ?.also { user ->
                Log.login(
                    webClientId = webClientId,
                    userAnalyticsId = user.analyticsId,
                    idProvider = idProvider
                )
            }
            ?: createUser()
    }

    suspend fun UserId.fetchUser(): User? = firestoreClient.get(users / this)
}