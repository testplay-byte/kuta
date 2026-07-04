package eu.kanade.tachiyomi.data.anilist

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * FORK: Phase 3 — AniList repository.
 * Wraps [AniListApi] with coroutine-friendly suspend functions + simple in-memory caching.
 *
 * Registered as an Injekt singleton in AppModule. Used by the home screen's
 * ViewModel to fetch browse data (trending, popular, seasonal, top-rated, genre).
 *
 * Caching: each list is cached in-memory for [CACHE_TTL_MS] (5 min). The cache
 * is per-section and cleared on app restart. Sufficient for browse — no need
 * for disk persistence at this stage.
 */
class AniListRepository(
    private val api: AniListApi,
) {
    private val cache = mutableMapOf<String, CacheEntry<List<AniListMedia>>>()

    suspend fun getTrending(): Result<List<AniListMedia>> = fetch(TRENDING_KEY) { api.getTrending() }
    suspend fun getPopular(): Result<List<AniListMedia>> = fetch(POPULAR_KEY) { api.getPopular() }
    suspend fun getSeasonal(): Result<List<AniListMedia>> = fetch(SEASONAL_KEY) { api.getSeasonal() }
    suspend fun getTopRated(): Result<List<AniListMedia>> = fetch(TOP_RATED_KEY) { api.getTopRated() }

    suspend fun getByGenre(genre: String): Result<List<AniListMedia>> =
        fetch("${GENRE_PREFIX}$genre") { api.getByGenre(genre) }

    /** Clear all cached data (e.g., on pull-to-refresh). */
    fun clearCache() {
        synchronized(cache) { cache.clear() }
    }

    private suspend fun fetch(key: String, block: () -> List<AniListMedia>): Result<List<AniListMedia>> =
        withContext(Dispatchers.IO) {
            // Check cache
            synchronized(cache) {
                cache[key]?.let { entry ->
                    if (System.currentTimeMillis() - entry.timestamp < CACHE_TTL_MS) {
                        return@withContext Result.success(entry.data)
                    }
                }
            }
            // Fetch fresh
            try {
                val data = block()
                synchronized(cache) { cache[key] = CacheEntry(data, System.currentTimeMillis()) }
                Result.success(data)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private data class CacheEntry<T>(val data: T, val timestamp: Long)

    companion object {
        private const val CACHE_TTL_MS = 5 * 60 * 1000L // 5 minutes
        private const val TRENDING_KEY = "trending"
        private const val POPULAR_KEY = "popular"
        private const val SEASONAL_KEY = "seasonal"
        private const val TOP_RATED_KEY = "top_rated"
        private const val GENRE_PREFIX = "genre:"
    }
}
