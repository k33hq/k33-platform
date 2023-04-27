package com.k33.platform.user

import io.firestore4k.typed.FirestoreClient
import io.firestore4k.typed.div
import java.util.*

object UserService {

    private val firestoreClient by lazy { FirestoreClient() }

    suspend fun UserId.createUser(email: String): User? {
        suspend fun createUser(): User? {
            firestoreClient.put(
                users / this,
                User(
                    userId = value,
                    analyticsId = UUID.randomUUID().toString()
                )
            )
            UserEventHandler.onNewUserCreated(email = email)
            return fetchUser()
        }
        return fetchUser() ?: createUser()
    }

    suspend fun UserId.fetchUser(): User? = firestoreClient.get(users / this)
}