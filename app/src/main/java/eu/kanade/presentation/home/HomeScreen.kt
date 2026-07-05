package eu.kanade.presentation.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.data.anilist.AniListAiringSchedule
import eu.kanade.tachiyomi.data.anilist.AniListMedia
import eu.kanade.tachiyomi.data.anilist.AniListRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * FORK: Phase 3 — Home screen (AniList-powered browse).
 * Replaces [eu.kanade.tachiyomi.ui.home.PlaceholderHomeScreen].
 *
 * Layout: top bar (logo + search + settings) → hero carousel (trending) →
 * content rows (trending, seasonal, all-time popular) → browse-by-genre grid.
 *
 * All UI uses [eu.kanade.tachiyomi.presentation...kuta.components.Kuta*] components
 * so the screen reskins when the user switches design language.
 *
 * Sections load independently (each has its own loading/error state) — a slow
 * trending query doesn't block the genre grid.
 */
object HomeScreen : Screen() {

    @Composable
    override fun Content() {
        val screenModel = rememberScreenModel { HomeScreenModel(Injekt.get()) }
        HomeScreenContent(screenModel)
    }
}

/**
 * ViewModel for [HomeScreen]. Fetches all 4 browse sections in parallel on init.
 */
class HomeScreenModel(
    private val repository: AniListRepository,
) : ScreenModel {

    private val _trending = MutableStateFlow<SectionState<List<AniListMedia>>>(SectionState.Loading)
    val trending: StateFlow<SectionState<List<AniListMedia>>> = _trending.asStateFlow()

    private val _seasonal = MutableStateFlow<SectionState<List<AniListMedia>>>(SectionState.Loading)
    val seasonal: StateFlow<SectionState<List<AniListMedia>>> = _seasonal.asStateFlow()

    private val _popular = MutableStateFlow<SectionState<List<AniListMedia>>>(SectionState.Loading)
    val popular: StateFlow<SectionState<List<AniListMedia>>> = _popular.asStateFlow()

    private val _airing = MutableStateFlow<SectionState<List<AniListAiringSchedule>>>(SectionState.Loading)
    val airing: StateFlow<SectionState<List<AniListAiringSchedule>>> = _airing.asStateFlow()

    init {
        loadAll()
    }

    fun loadAll() {
        screenModelScope.launch {
            repository.getTrending().fold(
                onSuccess = { _trending.value = SectionState.Success(it) },
                onFailure = { _trending.value = SectionState.Error(it.message ?: "Failed to load") },
            )
        }
        screenModelScope.launch {
            repository.getSeasonal().fold(
                onSuccess = { _seasonal.value = SectionState.Success(it) },
                onFailure = { _seasonal.value = SectionState.Error(it.message ?: "Failed to load") },
            )
        }
        screenModelScope.launch {
            repository.getPopular().fold(
                onSuccess = { _popular.value = SectionState.Success(it) },
                onFailure = { _popular.value = SectionState.Error(it.message ?: "Failed to load") },
            )
        }
        screenModelScope.launch {
            repository.getAiringSchedule().fold(
                onSuccess = { _airing.value = SectionState.Success(it) },
                onFailure = { _airing.value = SectionState.Error(it.message ?: "Failed to load") },
            )
        }
    }

    fun retryTrending() {
        _trending.value = SectionState.Loading
        screenModelScope.launch {
            repository.getTrending().fold(
                onSuccess = { _trending.value = SectionState.Success(it) },
                onFailure = { _trending.value = SectionState.Error(it.message ?: "Failed to load") },
            )
        }
    }

    fun retrySeasonal() {
        _seasonal.value = SectionState.Loading
        screenModelScope.launch {
            repository.getSeasonal().fold(
                onSuccess = { _seasonal.value = SectionState.Success(it) },
                onFailure = { _seasonal.value = SectionState.Error(it.message ?: "Failed to load") },
            )
        }
    }

    fun retryPopular() {
        _popular.value = SectionState.Loading
        screenModelScope.launch {
            repository.getPopular().fold(
                onSuccess = { _popular.value = SectionState.Success(it) },
                onFailure = { _popular.value = SectionState.Error(it.message ?: "Failed to load") },
            )
        }
    }

    fun retryAiring() {
        _airing.value = SectionState.Loading
        screenModelScope.launch {
            repository.getAiringSchedule().fold(
                onSuccess = { _airing.value = SectionState.Success(it) },
                onFailure = { _airing.value = SectionState.Error(it.message ?: "Failed to load") },
            )
        }
    }
}

/** Loadable section state. */
sealed interface SectionState<out T> {
    data object Loading : SectionState<Nothing>
    data class Error(val message: String) : SectionState<Nothing>
    data class Success<T>(val data: T) : SectionState<T>
}
