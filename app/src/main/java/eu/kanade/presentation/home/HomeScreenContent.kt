package eu.kanade.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import eu.kanade.tachiyomi.data.anilist.AniListMedia
import eu.kanade.tachiyomi.ui.setting.SettingsScreen
import kotlinx.coroutines.delay
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.kuta.components.KutaBadge
import tachiyomi.presentation.core.kuta.components.KutaBadgeVariant
import tachiyomi.presentation.core.kuta.components.KutaButton
import tachiyomi.presentation.core.kuta.components.KutaButtonVariant
import tachiyomi.presentation.core.kuta.components.KutaCard
import tachiyomi.presentation.core.kuta.components.KutaIconButton
import tachiyomi.presentation.core.kuta.components.KutaSkeleton
import tachiyomi.presentation.core.kuta.theme.LocalKutaColors
import tachiyomi.presentation.core.kuta.theme.LocalKutaTypography
import tachiyomi.presentation.core.util.collectAsState

/**
 * FORK: Phase 3 — Home screen content (AniList browse).
 * Uses Kuta* components throughout so it reskins per active design language.
 */
@Composable
fun HomeScreenContent(screenModel: HomeScreenModel) {
    val navigator = LocalNavigator.currentOrThrow
    val trending by screenModel.trending.collectAsState()
    val seasonal by screenModel.seasonal.collectAsState()
    val popular by screenModel.popular.collectAsState()
    val colors = LocalKutaColors.current
    val context = LocalContext.current

    Scaffold(
        containerColor = colors.bgBase,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.bgBase),
            contentPadding = PaddingValues(bottom = 80.dp),
        ) {
            // Top bar
            item { HomeTopBar(onSettingsClick = { navigator.push(SettingsScreen()) }) }

            // Hero carousel (trending)
            item {
                when (val state = trending) {
                    is SectionState.Loading -> HeroSkeleton()
                    is SectionState.Error -> SectionError("Couldn't load trending", state.message) { screenModel.retryTrending() }
                    is SectionState.Success -> {
                        val heroItems = state.data.take(5)
                        if (heroItems.isNotEmpty()) {
                            HeroCarousel(items = heroItems) { media ->
                                // FORK: Phase 3 — card tap is a no-op for now (detail screen is future phase)
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
                    is SectionState.Success -> AnimeCardRow(items = state.data) { media ->
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
                    is SectionState.Success -> AnimeCardRow(items = state.data) { media ->
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
                    is SectionState.Success -> AnimeCardRow(items = state.data) { media ->
                        android.widget.Toast.makeText(context, "Tapped: ${media.displayTitle}", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }

            // Browse by Genre
            item { SectionHeader("Browse by Genre") }
            item { GenreGrid { genre ->
                android.widget.Toast.makeText(context, "Genre: $genre (coming soon)", android.widget.Toast.LENGTH_SHORT).show()
            } }
        }
    }
}

// ===== Top Bar =====

@Composable
private fun HomeTopBar(onSettingsClick: () -> Unit) {
    val colors = LocalKutaColors.current
    val typography = LocalKutaTypography.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Logo / app name
        Text(
            text = "Kuta",
            style = typography.display?.copy(fontSize = 24.sp) ?: MaterialTheme.typography.titleLarge,
            color = colors.accentPrimary,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.width(16.dp))
        // Search bar (placeholder — tappable, no-op for now)
        Row(
            modifier = Modifier
                .weight(1f)
                .height(40.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(colors.bgElevated)
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
            Text(
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

    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Column {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(16.dp)),
            ) { page ->
                HeroSlide(items[page], onTap)
            }
            Spacer(Modifier.height(8.dp))
            // Dot indicators
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
}

@Composable
private fun HeroSlide(media: AniListMedia, onTap: (AniListMedia) -> Unit) {
    val colors = LocalKutaColors.current
    val typography = LocalKutaTypography.current
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize().clickable { onTap(media) }) {
        // Banner image
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
            Text(
                text = media.displayTitle,
                style = typography.headline,
                color = colors.fgPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (media.seasonYear != null) {
                    Text("${media.seasonYear}", style = typography.monoValue ?: typography.body, color = colors.fgSecondary)
                    Spacer(Modifier.width(8.dp))
                }
                if (media.formatDisplay.isNotEmpty()) {
                    Text(media.formatDisplay, style = typography.monoValue ?: typography.body, color = colors.fgSecondary)
                    Spacer(Modifier.width(8.dp))
                }
                if (media.averageScore != null) {
                    androidx.compose.material3.Icon(Icons.Filled.Star, contentDescription = null, tint = colors.accentPrimary, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(2.dp))
                    Text("${media.averageScore}", style = typography.monoValue ?: typography.body, color = colors.fgSecondary)
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
                    text = "My List",
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
            .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = typography.title,
            color = colors.fgPrimary,
            modifier = Modifier.weight(1f),
        )
        Text(
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
    val context = LocalContext.current

    KutaCard(
        modifier = Modifier
            .width(130.dp)
            .clickable { onTap(media) },
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(173.dp), // 3:4 aspect for 130dp width
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
                            text = "${media.averageScore}",
                            variant = KutaBadgeVariant.ACCENT,
                        )
                    }
                }
            }
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = media.displayTitle,
                    style = typography.bodySmall,
                    color = colors.fgPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (media.formatDisplay.isNotEmpty() || media.seasonYear != null) {
                    val meta = listOfNotNull(
                        media.formatDisplay.takeIf { it.isNotEmpty() },
                        media.seasonYear?.toString(),
                    ).joinToString(" • ")
                    Text(
                        text = meta,
                        style = typography.monoValue ?: typography.bodySmall,
                        color = colors.fgMuted,
                        fontSize = 10.sp,
                    )
                }
            }
        }
    }
}

// ===== Genre Grid =====

@Composable
private fun GenreGrid(onGenreTap: (String) -> Unit) {
    val genres = listOf(
        "Action" to Color(0xFFEF4444),
        "Adventure" to Color(0xFFF59E0B),
        "Comedy" to Color(0xFFEAB308),
        "Fantasy" to Color(0xFF8B5CF6),
        "Horror" to Color(0xFF6B7280),
        "Mystery" to Color(0xFF06B6D4),
        "Romance" to Color(0xFFEC4899),
        "Sci-Fi" to Color(0xFF3B82F6),
    )
    val rows = genres.chunked(2)
    rows.forEach { rowGenres ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            rowGenres.forEach { (name, color) ->
                GenreCard(name, color, Modifier.weight(1f), onGenreTap)
            }
            // Pad odd rows with a spacer so cards stay full-width
            if (rowGenres.size == 1) {
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun GenreCard(name: String, color: Color, modifier: Modifier = Modifier, onTap: (String) -> Unit) {
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
                        0f to color.copy(alpha = 0.7f),
                        1f to color.copy(alpha = 0.4f),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = name,
                style = typography.subtitle,
                color = Color.White,
                fontWeight = FontWeight.Bold,
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
        height = 200,
    )
}

@Composable
private fun CardRowSkeleton() {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(4) {
            KutaSkeleton(modifier = Modifier.width(130.dp), height = 220)
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
        Text(title, style = typography.subtitle, color = colors.accentTertiary)
        Spacer(Modifier.height(4.dp))
        Text(message, style = typography.bodySmall, color = colors.fgMuted)
        Spacer(Modifier.height(12.dp))
        KutaButton(text = "Retry", onClick = onRetry, variant = KutaButtonVariant.SECONDARY)
    }
}
