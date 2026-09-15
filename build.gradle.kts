package com.bitchord.player.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.bitchord.player.data.ThemeConfig
import com.bitchord.player.playback.PlayerBus
import com.bitchord.player.ui.components.MiniPlayer
import com.bitchord.player.ui.nav.BitChordPage
import com.bitchord.player.ui.pages.library.DeviceLibraryPage
import com.bitchord.player.ui.pages.soundcloud.SoundCloudPortalPage
import com.bitchord.player.ui.pages.theme.ThemeStudioPage
import com.bitchord.player.ui.pages.youtube.YouTubeListPage

/**
 * Root shell. The three required pages are hosted on unique routes derived from
 * [BitChordPage.pageId], with directional slide+fade transitions between them.
 */
@Composable
fun BitChordShell(
    themeConfig: ThemeConfig,
    onUpdateTheme: ((ThemeConfig) -> ThemeConfig) -> Unit,
    playerBus: PlayerBus,
    navController: NavHostController = rememberNavController()
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentPage = BitChordPage.fromRoute(backStackEntry?.destination?.route)
        ?: BitChordPage.YOUTUBE_LIST // fallback while backstack settles on first composition
    val now by playerBus.state.collectAsStateWithLifecycle()

    // Ordered list of bottom-nav destinations. Adjust icons/labels to match your
    // actual BitChordPage definition (title/icon fields assumed here).
    val navPages = listOf(
        BitChordPage.YOUTUBE_LIST,
        BitChordPage.SOUNDCLOUD_PORTAL,
        BitChordPage.DEVICE_LIBRARY,
        BitChordPage.THEME_STUDIO
    )

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(Modifier.weight(1f)) {
            NavHost(
                navController = navController,
                startDestination = BitChordPage.YOUTUBE_LIST.route,
                enterTransition = {
                    slideInHorizontally(tween(320)) { if (targetState.destination.route.isForwardFrom(navPages, initialState.destination.route)) it / 3 else -it / 3 } +
                        fadeIn(tween(280))
                },
                exitTransition = {
                    slideOutHorizontally(tween(300)) { if (targetState.destination.route.isForwardFrom(navPages, initialState.destination.route)) -it / 4 else it / 4 } +
                        fadeOut(tween(220))
                },
                popEnterTransition = {
                    slideInHorizontally(tween(320)) { if (targetState.destination.route.isForwardFrom(navPages, initialState.destination.route)) it / 3 else -it / 3 } +
                        fadeIn(tween(280))
                },
                popExitTransition = {
                    slideOutHorizontally(tween(300)) { if (targetState.destination.route.isForwardFrom(navPages, initialState.destination.route)) -it / 4 else it / 4 } +
                        fadeOut(tween(220))
                }
            ) {
                composable(BitChordPage.YOUTUBE_LIST.route) {
                    YouTubeListPage(playerBus = playerBus)
                }
                composable(BitChordPage.SOUNDCLOUD_PORTAL.route) {
                    SoundCloudPortalPage()
                }
                composable(BitChordPage.DEVICE_LIBRARY.route) {
                    DeviceLibraryPage()
                }
                composable(BitChordPage.THEME_STUDIO.route) {
                    ThemeStudioPage(config = themeConfig, onUpdate = onUpdateTheme)
                }
            }

            // Floating mini-player sits above every page (but under the bottom bar).
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(WindowInsets.navigationBars.asPaddingValues())
                    .padding(bottom = 78.dp),
                contentAlignment = androidx.compose.ui.Alignment.BottomCenter
            ) {
                MiniPlayer(
                    state = now,
                    visible = currentPage != BitChordPage.YOUTUBE_LIST,
                    onToggle = playerBus::togglePlayPause,
                    onStop = playerBus::stop,
                    onSeek = { f -> playerBus.seekTo((f * now.durationMs).toLong()) },
                    onExpand = { /* hook for a future full-screen now-playing sheet */ }
                )
            }
        }

        NavigationBar {
            navPages.forEach { page ->
                NavigationBarItem(
                    selected = currentPage == page,
                    onClick = {
                        if (currentPage != page) {
                            navController.navigate(page.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    icon = { Icon(page.icon, contentDescription = page.title) },
                    label = { Text(page.title) }
                )
            }
        }
    }
}

// Helper: determines slide direction based on each page's position in navPages.
private fun String?.isForwardFrom(pages: List<BitChordPage>, from: String?): Boolean {
    val toIndex = pages.indexOfFirst { it.route == this }
    val fromIndex = pages.indexOfFirst { it.route == from }
    return toIndex >= fromIndex
}