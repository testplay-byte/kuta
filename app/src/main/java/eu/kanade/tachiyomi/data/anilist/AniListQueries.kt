package eu.kanade.tachiyomi.data.anilist

/**
 * FORK: Phase 3 — AniList GraphQL queries.
 * Per the AniList API docs (https://docs.anilist.co).
 *
 * All queries are browse-only (no auth required). Each returns the standard
 * anime media fields defined in [AniListModels].
 */
object AniListQueries {

    /** Standard media field selection — used by all browse queries. */
    private const val MEDIA_FIELDS = """
        id
        idMal
        title { romaji english native }
        coverImage { medium large extraLarge color }
        bannerImage
        averageScore
        meanScore
        popularity
        favourites
        episodes
        duration
        genres
        format
        status
        season
        seasonYear
        startDate { year month day }
        endDate { year month day }
        description(asHtml: false)
        studios(isMain: true) { nodes { name } }
    """

    /** Trending anime (top 20). */
    const val TRENDING = """
        query Trending(${"$"}page: Int = 1, ${"$"}perPage: Int = 20) {
            Page(page: ${"$"}page, perPage: ${"$"}perPage) {
                media(sort: TRENDING_DESC, type: ANIME, isAdult: false) {
                    $MEDIA_FIELDS
                }
            }
        }
    """

    /** All-time popular (top 20). */
    const val POPULAR = """
        query Popular(${"$"}page: Int = 1, ${"$"}perPage: Int = 20) {
            Page(page: ${"$"}page, perPage: ${"$"}perPage) {
                media(sort: POPULARITY_DESC, type: ANIME, isAdult: false) {
                    $MEDIA_FIELDS
                }
            }
        }
    """

    /** Current-season popular (top 20). Season + year passed as variables. */
    const val SEASONAL = """
        query Seasonal(${"$"}season: MediaSeason, ${"$"}seasonYear: Int, ${"$"}page: Int = 1, ${"$"}perPage: Int = 20) {
            Page(page: ${"$"}page, perPage: ${"$"}perPage) {
                media(season: ${"$"}season, seasonYear: ${"$"}seasonYear, sort: POPULARITY_DESC, type: ANIME, isAdult: false) {
                    $MEDIA_FIELDS
                }
            }
        }
    """

    /** Top-rated (top 20). */
    const val TOP_RATED = """
        query TopRated(${"$"}page: Int = 1, ${"$"}perPage: Int = 20) {
            Page(page: ${"$"}page, perPage: ${"$"}perPage) {
                media(sort: SCORE_DESC, type: ANIME, isAdult: false) {
                    $MEDIA_FIELDS
                }
            }
        }
    """

    /** By genre (top 20). Genre passed as a variable. */
    const val BY_GENRE = """
        query ByGenre(${"$"}genre: String, ${"$"}page: Int = 1, ${"$"}perPage: Int = 20) {
            Page(page: ${"$"}page, perPage: ${"$"}perPage) {
                media(genre: ${"$"}genre, sort: POPULARITY_DESC, type: ANIME, isAdult: false) {
                    $MEDIA_FIELDS
                }
            }
        }
    """

    /** FORK: Phase 3 — Airing schedules for "Coming Up Next" section.
     * Returns upcoming episodes airing within the next 7 days, sorted by airing time.
     * Per https://docs.anilist.co (AiringSchedule type). */
    const val AIRING_SCHEDULE = """
        query AiringSchedule(${"$"}airingAt: Int!, ${"$"}page: Int = 1, ${"$"}perPage: Int = 20) {
            Page(page: ${"$"}page, perPage: ${"$"}perPage) {
                airingSchedules(airingAt_greater: ${"$"}airingAt, sort: TIME) {
                    id
                    airingAt
                    timeUntilAiring
                    episode
                    media {
                        $MEDIA_FIELDS
                    }
                }
            }
        }
    """
}
