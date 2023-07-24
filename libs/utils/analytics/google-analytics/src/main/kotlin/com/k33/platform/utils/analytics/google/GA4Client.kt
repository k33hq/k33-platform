package com.k33.platform.utils.analytics.google

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.k33.platform.utils.config.loadConfig
import com.k33.platform.utils.logging.getLogger
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory

sealed class GA4Client {

    private val logger by getLogger()

    protected val config: Config by loadConfig("googleAnalytics", "googleAnalytics")

    protected open val httpClient = HttpClient(CIO) {
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.ALL
        }
        install(UserAgent) {
            agent = "k33-backend"
        }
        install(ContentNegotiation) {
            jackson {
                setSerializationInclusion(JsonInclude.Include.NON_NULL)
                setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
            }
        }
        defaultRequest {
            url(scheme = "https", host = "www.google-analytics.com", path = "/mp/collect") {
                parameters.append("api_secret", config.apiKey)
            }
            contentType(ContentType.Application.Json)
        }
    }

    protected suspend fun validate(request: Request): Boolean {
        if (!request.verify()) {
            return false
        }
        val validationResponse: ValidationResponse = withContext(Dispatchers.IO) {
            httpClient.post {
                url(path = "/debug/mp/collect")
                setBody(request)
            }
        }.body()
        if (validationResponse.validationMessages.isNotEmpty()) {
            logger.error(validationResponse.validationMessages.joinToString())
            return false
        }
        return true
    }

    protected suspend fun submit(request: Request) {
        withContext(Dispatchers.IO) {
            httpClient.post {
                setBody(request)
            }.body<Unit>()
        }
    }
}

data object GA4ClientForApp : GA4Client() {

    override val httpClient = super.httpClient.config {
        defaultRequest {
            url {
                parameters.append("firebase_app_id", config.firebaseAppId)
            }
        }
    }

    internal suspend fun validate(request: AppRequest) = super.validate(request)

    internal suspend fun submit(request: AppRequest) = super.submit(request)
}

data object GA4ClientForWeb : GA4Client() {

    override val httpClient = super.httpClient.config {
        defaultRequest {
            url {
                parameters.append("measurement_id", config.measurementId)
            }
        }
    }

    internal suspend fun validate(request: WebRequest) = super.validate(request)

    suspend fun submit(request: WebRequest) = super.submit(request)
}

/**
 * Ref: https://developers.google.com/analytics/devguides/collection/protocol/ga4/reference
 */
sealed class Request(
    @get:JsonProperty("user_id")
    val userAnalyticsId: String?,
    @get:JsonProperty("timestamp_micros")
    val timestampInMicroseconds: Long?,
    @get:JsonProperty("user_properties")
    val userProperties: Map<String, String> = emptyMap(),
    @get:JsonProperty("non_personalized_ads")
    val nonPersonalizedAds: Boolean = true,
    val events: Collection<Event>,
)

class WebRequest(
    @JsonProperty("client_id")
    val webClientId: String,
    userAnalyticsId: String? = null,
    timestampInMicroseconds: Long? = null,
    events: Collection<Event>,
) : Request(
    userAnalyticsId = userAnalyticsId,
    timestampInMicroseconds = timestampInMicroseconds,
    events = events,
)

class AppRequest(
    @JsonProperty("app_instance_id")
    val appInstanceId: String,
    userAnalyticsId: String? = null,
    timestampInMicroseconds: Long? = null,
    events: Collection<Event>,
) : Request(
    userAnalyticsId = userAnalyticsId,
    timestampInMicroseconds = timestampInMicroseconds,
    events = events,
)

private data class ValidationResponse(
    val validationMessages: List<ValidationMessage>
)

private data class ValidationMessage(
    val fieldPath: String? = null,
    val description: String? = null,
    val validationCode: ValidationCode? = null,
)

enum class ValidationCode {
    VALUE_INVALID,
    VALUE_REQUIRED,
    NAME_INVALID,
    NAME_RESERVED,
    VALUE_OUT_OF_BOUNDS,
    EXCEEDED_MAX_ENTITIES,
    NAME_DUPLICATED,
    INTERNAL_ERROR,
}

private val logger by lazy {
    LoggerFactory.getLogger(Request::class.java)
}

internal fun Request.verify(): Boolean {
    val validationErrors = mutableSetOf<String>()

    if (events.size > 25) {
        validationErrors += "Requests can have a maximum of 25 events."
    }

    val regex = Regex("^[a-zA-Z][a-zA-Z0-9_]{0,39}$")
    fun verifyEventNames() {
        val eventNameSet = events.map(Event::name).toSet()
        val foundInvalidEventNames = eventNameSet.filterNot { it.matches(regex) }
        if (foundInvalidEventNames.isNotEmpty()) {
            validationErrors += "Found invalid events names: $foundInvalidEventNames"
        }
        val foundWithReservedNames = eventNameSet.intersect(reservedEventNames)
        if (foundWithReservedNames.isNotEmpty()) {
            validationErrors += "Found events with reserved names: $foundWithReservedNames"
        }
    }

    fun verifyUserProperties() {
        if (userProperties.isNotEmpty()) {
            if (userProperties.size > 25) {
                validationErrors += "Events can have a maximum of 25 user properties."
            }
            val foundLongUserPropertyNames = userProperties.keys.filter { it.length > 24 }
            if (foundLongUserPropertyNames.isNotEmpty()) {
                validationErrors += "User property names must be 24 characters or fewer. Found $foundLongUserPropertyNames"
            }
            val foundLongUserPropertyValues = userProperties.values.filter { it.length > 36 }
            if (foundLongUserPropertyValues.isNotEmpty()) {
                validationErrors += "User property values must be 36 characters or fewer. Found $foundLongUserPropertyValues"
            }
            val foundWithReservedNames = userProperties.keys.map(String::lowercase).toSet() - reservedUserPropertyNames
            if (foundWithReservedNames.isNotEmpty()) {
                validationErrors += "Found userProperties with reserved names: $foundWithReservedNames"
            }
            val foundWithReservedPrefix = userProperties.keys.map(String::lowercase).toSet().filter {
                reservedUserPropertyNamesPrefix.any { prefix -> it.startsWith(prefix) }
            }
            if (foundWithReservedPrefix.isNotEmpty()) {
                validationErrors += "Found userProperties with reserved prefix: $foundWithReservedNames"
            }
        }
    }

    verifyEventNames()

    if (validationErrors.isNotEmpty()) {
        logger.error(validationErrors.joinToString())
        return false
    }
    return true
}

private val reservedEventNames = setOf(
    "ad_activeview",
    "ad_click",
    "ad_exposure",
    "ad_impression",
    "ad_query",
    "adunit_exposure",
    "app_clear_data",
    "app_install",
    "app_update",
    "app_remove",
    "error",
    "first_open",
    "first_visit",
    "in_app_purchase",
    "notification_dismiss",
    "notification_foreground",
    "notification_open",
    "notification_receive",
    "os_update",
    "screen_view",
    "session_start",
    "user_engagement",
)

private const val reservedEventParamName = "firebase_conversion"

private val reservedEventParamNamesPrefix = setOf(
    "google_",
    "ga_",
    "firebase_",
)

private val reservedUserPropertyNames = setOf(
    "first_open_time",
    "first_visit_time",
    "last_deep_link_referrer",
    "user_id",
    "first_open_after_install",
)

private val reservedUserPropertyNamesPrefix = setOf(
    "google_",
    "ga_",
    "firebase_",
)