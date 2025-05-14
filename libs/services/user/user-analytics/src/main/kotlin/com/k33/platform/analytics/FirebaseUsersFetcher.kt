package com.k33.platform.analytics

import com.google.firebase.FirebaseApp
import com.google.firebase.auth.DeleteUsersResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ListUsersPage
import com.k33.platform.google.coroutine.ktx.await
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.time.Duration.Companion.seconds

@Serializable
data class User(
    val userId: String,
    val email: String,
    val idProviders: List<String>,
    val createdOn: String,
    val lastSignIn: String,
    val lastActive: String,
)

object FirebaseUsersFetcher {

    private val firebaseAuth: FirebaseAuth by lazy {
        val firebaseApp = FirebaseApp.initializeApp("user-admin")
        FirebaseAuth.getInstance(firebaseApp)
    }

    suspend fun fetchUsers(): List<User> {
        val page: ListUsersPage? = firebaseAuth
            .listUsersAsync(null)
            .await()

        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        fun Long.toDateString(): String {
            return Instant
                .ofEpochMilli(this)
                .atZone(ZoneId.of("UTC"))
                .format(dateFormatter)
        }

        val users = flow {
            var localPage: ListUsersPage? = page

            while (localPage != null) {
                for (user in localPage.values) {
                    emit(
                        User(
                            userId = user.uid,
                            email = user.email,
                            idProviders = user.providerData.map { it.providerId },
                            createdOn = user.userMetadata.creationTimestamp.toDateString(),
                            lastSignIn = user.userMetadata.lastSignInTimestamp.toDateString(),
                            lastActive = user.userMetadata.lastRefreshTimestamp.toDateString(),
                        )
                    )
                }
                localPage = localPage.nextPage
            }
        }
        return users.toList()
    }

    internal suspend fun deleteUsers(
        userIdList: List<String>
    ): List<DeleteUsersResult> {
        return userIdList
            .chunked(1000)
            .asFlow()
            .map { userIds ->
                firebaseAuth.deleteUsers(userIds).also { delay(1.seconds) }
            }
            .toList()
    }
}

fun main() {
    runBlocking {
        FirebaseUsersFetcher
            .fetchUsers()
            .map(User::email)
            .forEach { email ->
                println(email)
            }
    }
}