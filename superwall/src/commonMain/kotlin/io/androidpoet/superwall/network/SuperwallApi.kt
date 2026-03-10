package io.androidpoet.superwall.network

import io.androidpoet.superwall.models.SuperwallConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable

/**
 * HTTP client for Superwall's backend API.
 * Handles configuration fetching, event submission, and assignment retrieval.
 */
public class SuperwallApi(
  private val httpClient: HttpClient,
  private val baseUrl: String,
  private val apiKey: String,
) {

  /**
   * Fetch the remote configuration (paywalls, triggers, product mappings).
   */
  public suspend fun getConfig(): SuperwallConfig {
    return httpClient.get("$baseUrl/api/v1/config") {
      header("Authorization", "Bearer $apiKey")
    }.body()
  }

  /**
   * Submit analytics events in batches.
   */
  public suspend fun postEvents(events: List<EventPayload>) {
    httpClient.post("$baseUrl/api/v1/events") {
      header("Authorization", "Bearer $apiKey")
      contentType(ContentType.Application.Json)
      setBody(EventBatch(events = events))
    }
  }

  /**
   * Get experiment assignments for the current user.
   */
  public suspend fun getAssignments(): AssignmentsResponse {
    return httpClient.get("$baseUrl/api/v1/assignments") {
      header("Authorization", "Bearer $apiKey")
    }.body()
  }

  /**
   * Confirm all experiment assignments (triggers server-side logging).
   */
  public suspend fun confirmAssignments(assignments: ConfirmAssignmentsRequest) {
    httpClient.post("$baseUrl/api/v1/assignments/confirm") {
      header("Authorization", "Bearer $apiKey")
      contentType(ContentType.Application.Json)
      setBody(assignments)
    }
  }
}

@Serializable
public data class EventPayload(
  val name: String,
  val params: Map<String, String> = emptyMap(),
  val createdAt: String,
)

@Serializable
public data class EventBatch(
  val events: List<EventPayload>,
)

@Serializable
public data class AssignmentsResponse(
  val assignments: Map<String, String> = emptyMap(),
)

@Serializable
public data class ConfirmAssignmentsRequest(
  val assignments: Map<String, String>,
)
