package eu.kanade.tachiyomi.data.anilist

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * FORK: Phase 3 — AniList data models.
 * Per the AniList API docs (https://docs.anilist.co).
 *
 * These map 1:1 to the GraphQL response shape (with `ignoreUnknownKeys = true`
 * so extra fields don't break parsing).
 */

@Serializable
data class AniListPageResponse(
    val data: PageData? = null,
)

@Serializable
data class PageData(
    val Page: MediaPage? = null,
)

@Serializable
data class MediaPage(
    val media: List<AniListMedia> = emptyList(),
    val airingSchedules: List<AniListAiringSchedule> = emptyList(),
)

/** FORK: Phase 3 — Airing schedule for "Coming Up Next" section. */
@Serializable
data class AniListAiringSchedule(
    val id: Long,
    val airingAt: Long,
    val timeUntilAiring: Long,
    val episode: Int,
    val media: AniListMedia? = null,
)

@Serializable
data class AniListMedia(
    val id: Long,
    @SerialName("idMal") val malId: Long? = null,
    val title: AniListTitle? = null,
    val coverImage: AniListCoverImage? = null,
    val bannerImage: String? = null,
    val averageScore: Int? = null,
    val meanScore: Int? = null,
    val popularity: Int? = null,
    val favourites: Int? = null,
    val episodes: Int? = null,
    val duration: Int? = null,
    val genres: List<String> = emptyList(),
    val format: String? = null,
    val status: String? = null,
    val season: String? = null,
    val seasonYear: Int? = null,
    val startDate: AniListFuzzyDate? = null,
    val endDate: AniListFuzzyDate? = null,
    val description: String? = null,
    val studios: AniListStudioConnection? = null,
) {
    /** Preferred display title: english if present, else romaji, else native. */
    val displayTitle: String
        get() = title?.english?.takeIf { it.isNotBlank() }
            ?: title?.romaji?.takeIf { it.isNotBlank() }
            ?: title?.native?.takeIf { it.isNotBlank() }
            ?: "Unknown"

    /** Best-available cover image URL (largest first). */
    val coverUrl: String?
        get() = coverImage?.extraLarge ?: coverImage?.large ?: coverImage?.medium

    /** Description truncated to 200 chars (per spec). */
    val shortDescription: String
        get() = description?.let {
            // Strip HTML-ish tags (AniList description can contain <br>, <i>, etc.)
            val plain = it.replace(Regex("<[^>]+>"), "").replace("&amp;", "&")
                .replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&#39;", "'")
            if (plain.length > 200) plain.take(197) + "…" else plain
        } ?: ""

    /** Main studio name (first main studio), or null. */
    val studio: String?
        get() = studios?.nodes?.firstOrNull()?.name

    /** Format display string (TV, MOVIE, OVA, etc.). */
    val formatDisplay: String
        get() = when (format) {
            "TV" -> "TV"
            "TV_SHORT" -> "TV Short"
            "MOVIE" -> "Movie"
            "SPECIAL" -> "Special"
            "OVA" -> "OVA"
            "ONA" -> "ONA"
            "MUSIC" -> "Music"
            else -> format ?: ""
        }
}

@Serializable
data class AniListTitle(
    val romaji: String? = null,
    val english: String? = null,
    val native: String? = null,
)

@Serializable
data class AniListCoverImage(
    val medium: String? = null,
    val large: String? = null,
    val extraLarge: String? = null,
    val color: String? = null,
)

@Serializable
data class AniListFuzzyDate(
    val year: Int? = null,
    val month: Int? = null,
    val day: Int? = null,
)

@Serializable
data class AniListStudioConnection(
    val nodes: List<AniListStudio> = emptyList(),
)

@Serializable
data class AniListStudio(
    val name: String? = null,
)
