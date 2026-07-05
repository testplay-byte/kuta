package eu.kanade.tachiyomi.data.anilist

import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.jsonMime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import uy.kohesive.injekt.injectLazy
import java.time.LocalDate
import java.time.Month

/**
 * FORK: Phase 3 — AniList GraphQL API client.
 * Per https://docs.anilist.co.
 *
 * Uses Aniyomi's existing [OkHttpClient] (via Injekt) + [Json] parser.
 * No auth required for browse queries — guest browse only (per Phase 3 scope).
 *
 * Rate limit: AniList allows 90 requests/minute (anonymous). We shouldn't hit
 * this for browse (5 queries on home screen load). If we do, the API returns
 * 429 and we surface it as a failure to the repository.
 */
class AniListApi(
    private val client: OkHttpClient,
    private val json: Json,
) {
    private val jsonParser: Json by injectLazy()

    /** Execute a GraphQL query with optional variables. Returns the parsed [AniListPageResponse]. */
    private fun request(query: String, variables: Map<String, Any?> = emptyMap()): AniListPageResponse {
        val body = buildJsonObject {
            put("query", query)
            if (variables.isNotEmpty()) {
                put("variables", JsonObject(variables.mapValues { (_, v) ->
                    when (v) {
                        is String -> kotlinx.serialization.json.JsonPrimitive(v)
                        is Number -> kotlinx.serialization.json.JsonPrimitive(v)
                        is Boolean -> kotlinx.serialization.json.JsonPrimitive(v)
                        null -> kotlinx.serialization.json.JsonNull
                        else -> kotlinx.serialization.json.JsonPrimitive(v.toString())
                    }
                }))
            }
        }.toString().toRequestBody(jsonMime)

        return try {
            val response = client.newCall(POST(API_URL, body = body)).execute()
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw AniListException("HTTP ${response.code}: ${responseBody.take(200)}")
            }
            json.decodeFromString(AniListPageResponse.serializer(), responseBody)
        } catch (e: Exception) {
            throw AniListException("AniList request failed: ${e.message}", e)
        }
    }

    /** Trending anime (top 20). */
    fun getTrending(): List<AniListMedia> =
        request(AniListQueries.TRENDING).data?.Page?.media.orEmpty()

    /** All-time popular (top 20). */
    fun getPopular(): List<AniListMedia> =
        request(AniListQueries.POPULAR).data?.Page?.media.orEmpty()

    /** Current-season popular (top 20). Season + year auto-computed from today's date. */
    fun getSeasonal(): List<AniListMedia> {
        val today = LocalDate.now()
        val season = today.toAniListSeason()
        val year = today.year
        return request(
            AniListQueries.SEASONAL,
            mapOf("season" to season, "seasonYear" to year),
        ).data?.Page?.media.orEmpty()
    }

    /** Top-rated (top 20). */
    fun getTopRated(): List<AniListMedia> =
        request(AniListQueries.TOP_RATED).data?.Page?.media.orEmpty()

    /** By genre (top 20). */
    fun getByGenre(genre: String): List<AniListMedia> =
        request(AniListQueries.BY_GENRE, mapOf("genre" to genre)).data?.Page?.media.orEmpty()

    /** FORK: Phase 3 — Upcoming airing schedules (for "Coming Up Next"). Returns the next ~20 episodes. */
    fun getAiringSchedule(): List<AniListAiringSchedule> =
        request(
            AniListQueries.AIRING_SCHEDULE,
            mapOf("airingAt" to (System.currentTimeMillis() / 1000)),
        ).data?.Page?.airingSchedules.orEmpty()

    companion object {
        private const val API_URL = "https://graphql.anilist.co"

        /** Map a [LocalDate] to the AniList season string (WINTER/SPRING/SUMMER/FALL). */
        private fun LocalDate.toAniListSeason(): String = when (month) {
            Month.DECEMBER, Month.JANUARY, Month.FEBRUARY -> "WINTER"
            Month.MARCH, Month.APRIL, Month.MAY -> "SPRING"
            Month.JUNE, Month.JULY, Month.AUGUST -> "SUMMER"
            Month.SEPTEMBER, Month.OCTOBER, Month.NOVEMBER -> "FALL"
            else -> "WINTER"
        }
    }
}

/** Thrown when an AniList API request fails. */
class AniListException(message: String, cause: Throwable? = null) : Exception(message, cause)
