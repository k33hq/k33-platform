package com.k33.platform.cms.content

import kotlinx.serialization.json.JsonObject

interface Content {
    suspend fun fetch(entityId: String): JsonObject?
    suspend fun fetchAll(): Collection<JsonObject>
    suspend fun fetchIds(): List<String>
    suspend fun fetchIdToModifiedMap(): Map<String, String>
}