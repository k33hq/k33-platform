package com.k33.platform.utils.graphql

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.k33.platform.identity.auth.gcp.UserInfo
import com.k33.platform.utils.logging.logWithMDC
import graphql.ExecutionInput
import graphql.ExecutionResult
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.future.await
import kotlinx.serialization.Serializable

fun Application.module() {

    val graphQL by lazy { GraphqlModulesRegistry.getGraphQL() }

    val sdl by lazy { GraphqlModulesRegistry.getSdl() }

    val jacksonObjectMapper by lazy { jacksonObjectMapper() }

    routing {

        get("sdl") {
            call.respondText(sdl)
        }

        authenticate("esp-v2-header") {
            suspend fun handleRequest(
                call: ApplicationCall,
                graphqlQuery: String,
                userId: String,
            ) {
                val executionInput: ExecutionInput = ExecutionInput.newExecutionInput()
                    .query(graphqlQuery)
                    .graphQLContext(mapOf("userId" to userId))
                    .build()

                val executionResult: ExecutionResult = graphQL.executeAsync(executionInput).await()
                val data = jacksonObjectMapper.writeValueAsString(executionResult.getData())
                call.respond(
                    GraphqlResponse(
                        data = data,
                        errors = if (executionResult.errors.isNullOrEmpty()) {
                            null
                        } else {
                            executionResult.errors.map { it.toString() }
                        }
                    )
                )
            }
            get ("graphql") {
                val userId = call.principal<UserInfo>()!!.userId
                logWithMDC("userId" to userId) {
                    val graphqlQuery = call.request.queryParameters["query"]
                    if (graphqlQuery == null) {
                        call.respond(HttpStatusCode.BadRequest, "query param - 'query' is mandatory")
                    } else {
                        handleRequest(call, graphqlQuery, userId)
                    }
                }
            }
            post("graphql") {
                val userId = call.principal<UserInfo>()!!.userId
                logWithMDC("userId" to userId) {
                    val graphqlQuery = call.receive<GraphqlRequest>().query
                    handleRequest(call, graphqlQuery, userId)
                }
            }
        }
    }
}

@Serializable
data class GraphqlRequest(
    val query: String
)

@Serializable
data class GraphqlResponse(
    val data: String,
    val errors: List<String>? = null,
)