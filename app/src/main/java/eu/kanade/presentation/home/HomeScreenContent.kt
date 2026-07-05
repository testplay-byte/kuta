package eu.kanade.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import eu.kanade.tachiyomi.data.anilist.AniListAiringSchedule
import eu.kanade.tachiyomi.data.anilist.AniListMedia
import eu.kanade.tachiyomi.ui.setting.SettingsScreen
import kotlinx.coroutines.delay
import tachiyomi.presentation.core.kuta.components.KutaBadge
import tachiyomi.presentation.core.kuta.components.KutaBadgeVariant
import tachiyomi.presentation.core.kuta.components.KutaButton
import tachiyomi.presentation.core.kuta.components.KutaButtonVariant
import tachiyomi.presentation.core.kuta.components.KutaCard
import tachiyomi.presentation.core.kuta.components.KutaIconButton
import tachiyomi.presentation.core.kuta.components.KutaSkeleton
import tachiyomi.presentation.core.kuta.theme.LocalKutaColors
import tachiyomi.presentation.core.kuta.theme.LocalKutaTypography
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * FORK: Phase 3 — Home screen content (AniList browse).
 * Matches the reference website layout (https://1.pandn47859347.workers.dev/).
 * Uses Kuta* components throughout so it reskins per active design language.
 *
 * Layout: Haze-blurred top bar → hero carousel → 3 content rows →
 * Coming Up Next (airing schedule) → Browse by Genre (2-col grid).
 */
@Composable
fun HomeScreenContent(screenModel: HomeScreenModel) {
    val navigator = LocalNavigator.currentOrThrow
    val trending by screenModel.trending.collectAsState()
    val seasonal by screenModel.seasonal.collectAsState()
    val popular by screenModel.popular.collectAsState()
    val airing by screenModel.airing.collectAsState()
    val colors = LocalKutaColors.current
    val context = LocalContext.current
    val hazeState = remember { HazeState() }

    Box(modifier = Modifier.fillMaxSize().background(colors.bgBase)) {
        // Scrollable content (Haze source — gets blurred behind the top bar)
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .haze(hazeState, HazeStyle(tint = colors.bgBase.copy(alpha = 0.6f), blurRadius = 20.dp)),
            contentPadding = PaddingValues(bottom = 80.dp),
        ) {
            // Spacer for the top bar height (so content isn't hidden behind it initially)
            item { Spacer(Modifier.height(72.dp)) }

            // Hero carousel (trending)
            item {
                when (val state = trending) {
                    is SectionState.Loading -> HeroSkeleton()
                    is SectionState.Error -> SectionError("Couldn't load trending", state.message) { screenModel.retryTrending() }
                    is SectionState.Success<List<AniListMedia>> -> {
                        val heroItems = state.data.take(5)
                        if (heroItems.isNotEmpty()) {
                            HeroCarousel(items = heroItems) { media ->
                                android.widget.Toast.makeText(context, "Tapped: ${media.displayTitle}", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }

            // Trending Now row
            item { SectionHeader("Trending Now") }
            item {
                when (val state = trending) {
                    is SectionState.Loading -> CardRowSkeleton()
                    is SectionState.Error -> SectionError("Couldn't load trending", state.message) { screenModel.retryTrending() }
                    is SectionState.Success<List<AniListMedia>> -> AnimeCardRow(items = state.data) { media ->
                        android.widget.Toast.makeText(context, "Tapped: ${media.displayTitle}", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }

            // Popular This Season row
            item { SectionHeader("Popular This Season") }
            item {
                when (val state = seasonal) {
                    is SectionState.Loading -> CardRowSkeleton()
                    is SectionState.Error -> SectionError("Couldn't load seasonal", state.message) { screenModel.retrySeasonal() }
                    is SectionState.Success<List<AniListMedia>> -> AnimeCardRow(items = state.data) { media ->
                        android.widget.Toast.makeText(context, "Tapped: ${media.displayTitle}", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }

            // All-Time Popular row
            item { SectionHeader("All-Time Popular") }
            item {
                when (val state = popular) {
                    is SectionState.Loading -> CardRowSkeleton()
                    is SectionState.Error -> SectionError("Couldn't load popular", state.message) { screenModel.retryPopular() }
                    is SectionState.Success<List<AniListMedia>> -> AnimeCardRow(items = state.data) { media ->
                        android.widget.Toast.makeText(context, "Tapped: ${media.displayTitle}", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }

            // Coming Up Next (airing schedule) — NEW
            item { SectionHeader("Coming Up Next") }
            item {
                when (val state = airing) {
                    is SectionState.Loading -> AiringSkeleton()
                    is SectionState.Error -> SectionError("Couldn't load schedule", state.message) { screenModel.retryAiring() }
                    is SectionState.Success<List<AniListAiringSchedule>> -> {
                        AiringScheduleList(items = state.data.take(8)) { schedule ->
                            android.widget.Toast.makeText(context, "Tapped: ${schedule.media?.displayTitle ?: "Unknown"} Ep ${schedule.episode}", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            // Browse by Genre (2-col grid)
            item { SectionHeader("Browse by Genre") }
            item {
                GenreGrid { genre ->
                    android.widget.Toast.makeText(context, "Genre: $genre (coming soon)", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Top bar (Haze child — shows frosted blur of content scrolling under it)
        HomeTopBar(
            hazeState = hazeState,
            onSettingsClick = { navigator.push(SettingsScreen()) },
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}

// ===== Top Bar (Haze blur + status bar padding) =====

@Composable
private fun HomeTopBar(hazeState: HazeState, onSettingsClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalKutaColors.current
    val typography = LocalKutaTypography.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .hazeChild(hazeState, shape = RoundedCornerShape(0.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // "Kuta" logo — use display typography (Caveat in Notebook, Bold in others)
        androidx.compose.material3.Text(
            text = "Kuta",
            style = typography.display?.copy(fontSize = 26.sp) ?: androidx.compose.material3.MaterialTheme.typography.titleLarge,
            color = colors.accentPrimary,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.width(16.dp))
        // Search bar (translucent, rounded, tappable placeholder)
        Row(
            modifier = Modifier
                .weight(1f)
                .height(40.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(colors.bgElevated.copy(alpha = 0.7f))
                .clickable { /* FORK: Phase 3 — search is a future phase */ }
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            androidx.compose.material3.Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = "Search",
                tint = colors.fgMuted,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            androidx.compose.material3.Text(
                text = "Search anime…",
                style = typography.bodySmall,
                color = colors.fgMuted,
            )
        }
        Spacer(Modifier.width(8.dp))
        KutaIconButton(
            icon = Icons.Filled.Settings,
            onClick = onSettingsClick,
            contentDescription = "Settings",
        )
    }
}

// ===== Hero Carousel =====

@Composable
private fun HeroCarousel(items: List<AniListMedia>, onTap: (AniListMedia) -> Unit) {
    val colors = LocalKutaColors.current
    val typography = LocalKutaTypography.current
    val pagerState = rememberPagerState(pageCount = { items.size })

    // Auto-scroll every 6 seconds
    LaunchedEffect(items.size) {
        while (items.size > 1) {
            delay(6000)
            val nextPage = (pagerState.currentPage + 1) % items.size
            pagerState.animateScrollToPage(nextPage)
        }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(16.dp)),
        ) { page ->
            HeroSlide(items[page], onTap)
        }
        Spacer(Modifier.height(8.dp))
        // Dot indicators (active dot is wider)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            repeat(items.size) { index ->
                val isActive = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .height(6.dp)
                        .width(if (isActive) 18.dp else 6.dp)
                        .clip(CircleShape)
                        .background(if (isActive) colors.accentPrimary else colors.borderDefault),
                )
            }
        }
    }
}

@Composable
private fun HeroSlide(media: AniListMedia, onTap: (AniListMedia) -> Unit) {
    val colors = LocalKutaColors.current
    val typography = LocalKutaTypography.current
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize().clickable { onTap(media) }) {
        val imageUrl = media.bannerImage ?: media.coverUrl
        AsyncImage(
            model = imageUrl,
            contentDescription = media.displayTitle,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        // Gradient overlay (transparent top → bgBase bottom)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        1f to colors.bgBase,
                    ),
                ),
        )
        // Title + metadata + buttons at bottom
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
        ) {
            // Title: display typography (Caveat in Notebook)
            androidx.compose.material3.Text(
                text = media.displayTitle,
                style = typography.display?.copy(fontSize = 30.sp) ?: typography.headline,
                color = colors.fgPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            // Metadata row: year + format + score
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (media.seasonYear != null) {
                    androidx.compose.material3.Text("${media.seasonYear}", style = typography.monoValue ?: typography.body, color = colors.fgSecondary)
                    Spacer(Modifier.width(8.dp))
                }
                if (media.formatDisplay.isNotEmpty()) {
                    androidx.compose.material3.Text(media.formatDisplay, style = typography.monoValue ?: typography.body, color = colors.fgSecondary)
                    Spacer(Modifier.width(8.dp))
                }
                if (media.averageScore != null) {
                    androidx.compose.material3.Icon(Icons.Filled.Star, contentDescription = null, tint = colors.accentPrimary, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(2.dp))
                    androidx.compose.material3.Text("${media.averageScore}", style = typography.monoValue ?: typography.body, color = colors.fgSecondary)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KutaButton(
                    text = "Watch Now",
                    onClick = { android.widget.Toast.makeText(context, "Player coming soon", android.widget.Toast.LENGTH_SHORT).show() },
                    icon = Icons.Filled.PlayArrow,
                    variant = KutaButtonVariant.PRIMARY,
                )
                KutaButton(
                    text = "Add to List",
                    onClick = { android.widget.Toast.makeText(context, "Add to list coming soon", android.widget.Toast.LENGTH_SHORT).show() },
                    icon = Icons.Outlined.Add,
                    variant = KutaButtonVariant.SECONDARY,
                )
            }
        }
    }
}

// ===== Section Header =====

@Composable
private fun SectionHeader(title: String) {
    val colors = LocalKutaColors.current
    val typography = LocalKutaTypography.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            androidx.compose.material3.Text(
                text = title,
                style = typography.headline,
                color = colors.fgPrimary,
            )
            // Ink underline (semi-transparent accent bar, slightly rotated)
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .width(48.dp)
                    .height(3.dp)
                    .rotate(-0.5f)
                    .background(colors.accentPrimary.copy(alpha = 0.4f)),
            )
        }
        androidx.compose.material3.Text(
            text = "See All →",
            style = typography.bodySmall,
            color = colors.accentPrimary,
        )
    }
}

// ===== Anime Card Row =====

@Composable
private fun AnimeCardRow(items: List<AniListMedia>, onTap: (AniListMedia) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(items) { media ->
            AnimePosterCard(media, onTap)
        }
    }
}

@Composable
private fun AnimePosterCard(media: AniListMedia, onTap: (AniListMedia) -> Unit) {
    val colors = LocalKutaColors.current
    val typography = LocalKutaTypography.current

    KutaCard(
        modifier = Modifier
            .width(140.dp)
            .clickable { onTap(media) },
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f),
            ) {
                AsyncImage(
                    model = media.coverUrl,
                    contentDescription = media.displayTitle,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
                // Score badge (top-right)
                if (media.averageScore != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp),
                    ) {
                        KutaBadge(
                            text = "★ ${media.averageScore}",
                            variant = KutaBadgeVariant.ACCENT,
                        )
                    }
                }
                // TRENDING badge (top-left, washi-tape style)
                if (media.popularity != null && media.popularity > 50000) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(4.dp)
                            .rotate(-2f)
                            .clip(RoundedCornerShape(2.dp))
                            .background(colors.accentQuaternary.copy(alpha = 0.85f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        androidx.compose.material3.Text(
                            text = "TRENDING",
                            style = typography.label,
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
            Column(modifier = Modifier.padding(8.dp)) {
                androidx.compose.material3.Text(
                    text = media.displayTitle,
                    style = typography.body,
                    color = colors.fgPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.height(2.dp))
                // Metadata: episodes + genres
                val episodes = media.episodes
                val episodeText = if (episodes != null && episodes > 0) "$episodes Episodes" else "Ongoing"
                androidx.compose.material3.Text(
                    text = episodeText,
                    style = typography.bodySmall,
                    color = colors.fgMuted,
                    fontSize = 11.sp,
                )
                val topGenres = media.genres.take(2).joinToString(", ")
                if (topGenres.isNotEmpty()) {
                    androidx.compose.material3.Text(
                        text = topGenres,
                        style = typography.bodySmall,
                        color = colors.fgMuted,
                        fontSize = 11.sp,
                    )
                }
            }
        }
    }
}

// ===== Coming Up Next (Airing Schedule) =====

@Composable
private fun AiringScheduleList(items: List<AniListAiringSchedule>, onTap: (AniListAiringSchedule) -> Unit) {
    val colors = LocalKutaColors.current
    val typography = LocalKutaTypography.current
    val now = System.currentTimeMillis()

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        items.forEach { schedule ->
            AiringItem(schedule, now, onTap)
        }
    }
}

@Composable
private fun AiringItem(schedule: AniListAiringSchedule, now: Long, onTap: (AniListAiringSchedule) -> Unit) {
    val colors = LocalKutaColors.current
    val typography = LocalKutaTypography.current
    val context = LocalContext.current
    val media = schedule.media

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTap(schedule) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Thumbnail (56dp)
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.bgElevated),
        ) {
            AsyncImage(
                model = media?.coverUrl,
                contentDescription = media?.displayTitle,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
        Spacer(Modifier.width(12.dp))
        // Title + episode
        Column(modifier = Modifier.weight(1f)) {
            androidx.compose.material3.Text(
                text = media?.displayTitle ?: "Unknown",
                style = typography.body,
                color = colors.fgPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Medium,
            )
            androidx.compose.material3.Text(
                text = "Episode ${schedule.episode}",
                style = typography.bodySmall,
                color = colors.fgMuted,
            )
        }
        // Countdown badge
        val countdown = formatCountdown(schedule.timeUntilAiring, schedule.airingAt, now)
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(colors.accentPrimary.copy(alpha = 0.15f))
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            androidx.compose.material3.Text(
                text = countdown,
                style = typography.monoValue ?: typography.bodySmall,
                color = colors.accentPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

/** Format the countdown: "2h 30m", "Tomorrow 3:00 PM", or "3d 4h". */
private fun formatCountdown(timeUntilAiring: Long, airingAt: Long, now: Long): String {
    val seconds = timeUntilAiring
    val hours = TimeUnit.SECONDS.toHours(seconds)
    val minutes = TimeUnit.SECONDS.toMinutes(seconds) - hours * 60
    val days = TimeUnit.SECONDS.toDays(seconds)
    return when {
        days >= 2 -> "${days}d ${hours - days * 24}h"
        days == 1L -> "Tomorrow " + SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(airingAt * 1000))
        hours >= 1 -> "${hours}h ${minutes}m"
        else -> "${minutes}m"
    }
}

// ===== Genre Grid (2-column) =====

@Composable
private fun GenreGrid(onGenreTap: (String) -> Unit) {
    val genres = listOf(
        "Action" to listOf(Color(0xFFF97316), Color(0xFFEF4444)),       // orange→red
        "Adventure" to listOf(Color(0xFFD97706), Color(0xFFF97316)),    // amber→orange
        "Comedy" to listOf(Color(0xFFEAB308), Color(0xFFF59E0B)),        // yellow→amber
        "Fantasy" to listOf(Color(0xFFA78BFA), Color(0xFF8B5CF6)),       // violet→purple
        "Horror" to listOf(Color(0xFF6B7280), Color(0xFF374151)),        // gray→dark gray
        "Mystery" to listOf(Color(0xFF06B6D4), Color(0xFF3B82F6)),       // cyan→blue
        "Romance" to listOf(Color(0xFFFB7185), Color(0xFFEC4899)),       // rose→pink
        "Sci-Fi" to listOf(Color(0xFF3B82F6), Color(0xFF6366F1)),        // blue→indigo
    )
    val rows = genres.chunked(2)
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        rows.forEach { rowGenres ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                rowGenres.forEach { (name, gradient) ->
                    GenreCard(name, gradient, Modifier.weight(1f), onGenreTap)
                }
                if (rowGenres.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun GenreCard(name: String, gradient: List<Color>, modifier: Modifier = Modifier, onTap: (String) -> Unit) {
    val colors = LocalKutaColors.current
    val typography = LocalKutaTypography.current
    KutaCard(
        modifier = modifier
            .height(80.dp)
            .clickable { onTap(name) },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0f to gradient[0],
                        1f to gradient[1],
                    ),
                ),
        ) {
            // Washi tape decoration (top-center, rotated)
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 4.dp)
                    .width(60.dp)
                    .height(14.dp)
                    .rotate(-2f)
                    .background(colors.washiTape.copy(alpha = 0.7f))
                    .clip(RoundedCornerShape(2.dp)),
            )
            // Genre name (centered, white, bold)
            androidx.compose.material3.Text(
                text = name,
                style = typography.subtitle,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

// ===== Loading / Error States =====

@Composable
private fun HeroSkeleton() {
    KutaSkeleton(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        height = 220,
    )
}

@Composable
private fun CardRowSkeleton() {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(4) {
            KutaSkeleton(modifier = Modifier.width(140.dp), height = 220)
        }
    }
}

@Composable
private fun AiringSkeleton() {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        repeat(4) {
            KutaSkeleton(modifier = Modifier.fillMaxWidth(), height = 56)
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SectionError(title: String, message: String, onRetry: () -> Unit) {
    val colors = LocalKutaColors.current
    val typography = LocalKutaTypography.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        androidx.compose.material3.Text(title, style = typography.subtitle, color = colors.accentTertiary)
        Spacer(Modifier.height(4.dp))
        androidx.compose.material3.Text(message, style = typography.bodySmall, color = colors.fgMuted)
        Spacer(Modifier.height(12.dp))
        KutaButton(text = "Retry", onClick = onRetry, variant = KutaButtonVariant.SECONDARY)
    }
}
