package com.k33.platform.analytics

import com.k33.platform.user.UserId
import com.k33.platform.user.UserService.fetchUser
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking

fun main() {
    runBlocking {
        val allUsers = FirebaseUsersFetcher
            .fetchUsers()
        val nonRegisteredUsers = allUsers
            .map { user ->
                async {
                    if (UserId(user.userId).fetchUser() == null) user else null
                }
            }
            .awaitAll()
            .filterNotNull()

        println("${nonRegisteredUsers.size} of ${allUsers.size} users are in Firebase Auth, but not registered.")
//        nonRegisteredUsers.forEach { user ->
//            println("${user.createdOn},${user.lastSignIn},${user.lastActive}")
//        }
//        FirebaseUsersFetcher
//            .deleteUsers(nonRegisteredUsers.map(User::userId))
//            .forEach {
//                println("successCount: ${it.successCount} failureCount: ${it.failureCount}")
//            }
    }
}