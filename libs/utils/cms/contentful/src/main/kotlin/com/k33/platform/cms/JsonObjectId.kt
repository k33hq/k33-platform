package com.k33.platform.cms

import com.k33.platform.utils.algolia.Algolia
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

val JsonObject.objectIDString get() = get(Algolia.Key.ObjectID)?.jsonPrimitive?.contentOrNull

val JsonObject.objectID get() = objectIDString?.let(Algolia::ObjectID)
