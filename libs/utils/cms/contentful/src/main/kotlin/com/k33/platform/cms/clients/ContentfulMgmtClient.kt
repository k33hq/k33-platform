package com.k33.platform.cms.clients

import com.contentful.java.cma.CMAClient
import com.k33.platform.utils.logging.getLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ContentfulMgmtClient(
    spaceId: String,
    token: String,
) {
    private val logger by getLogger()

    private val client by lazy {
        CMAClient.Builder()
            .setAccessToken(token)
            .setSpaceId(spaceId)
            .build()
    }

    suspend fun <T> updateField(
        entryId: String,
        key: String,
        value: T,
    ): Boolean = withContext(Dispatchers.IO) {
        val entry = try {
            client
                .entries()
                .fetchOne(entryId)
        } catch (e: IllegalArgumentException) {
            logger.error("Entry not found: $entryId")
            return@withContext false
        }

        val locale = "en-US"
        val existingValue: T = entry.getField(
            key,
            locale
        )

        if (existingValue != value) {
            entry.setField(
                key,
                locale,
                value
            )

            val updatedEntry = client
                .entries()
                .update(entry)

            client
                .entries()
                .publish(updatedEntry)
            true
        } else {
            false
        }
    }
}

