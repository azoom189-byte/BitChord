                if (glassActive) {
                    // Liquid glass replaces the two stacked bars with the single
                    // component they are stacked to imitate: the now playing
                    // controls dock into the tab bar rather than riding above it,
                    // and the pair folds together on scroll. See [GlassNavBar].
                    GlassNavBar(
                        tabs = tabs,
                        selectedIndex = selectedTab,
                        onTabSelected = onTabSelected,
                        scrollConnection = navBarScroll,
                        song = player.song?.takeUnless { playerDocked },
                        isPlaying = player.isPlaying,
                        isLoading = player.isLoading,
                        onPlayPause = {
                            controller?.let { if (it.isPlaying) it.pause() else it.play() }
                        },
                        onNext = { controller?.seekToNextMediaItem() },
                        onExpand = { showNowPlaying = true },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .widthIn(max = FLOATING_BAR_MAX_WIDTH)
                            .fillMaxWidth(),
                    )
                } else Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        // Capped and centred rather than run to the page's edges
                        // — see [FLOATING_BAR_MAX_WIDTH]. It sits on the Column
                        // rather than on each bar so the two are held to the same
                        // width and keep the shared left and right edge they have
                        // on a phone. Before fillMaxWidth, so the fill has
                        // already been bounded by the time it is applied.
                        .widthIn(max = FLOATING_BAR_MAX_WIDTH)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Only where the player isn't already open beside the page
                    val activeSong = player.song
                    if (activeSong != null && !playerDocked) {
                        MiniPlayer(
                            song = activeSong,
                            isPlaying = player.isPlaying,
                            isLoading = player.isLoading,
                            onPlayPause = {
                                controller?.let { if (it.isPlaying) it.pause() else it.play() }
                            },
                            onNext = { controller?.seekToNextMediaItem() },
                            onClick = { showNowPlaying = true },
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }

                    FloatingBottomBar(
                        tabs = tabs,
                        selectedIndex = selectedTab,
                        onTabSelected = onTabSelected,
                        scrollConnection = navBarScroll,
                        modifier = Modifier.padding(bottom = 16.dp, start = 16.dp, end = 16.dp),
                    )
                }
            }

            // Dedicated side-by-side player pane when docked (e.g., tablet landscape)
            val currentSong = player.song
            if (playerDocked && currentSong != null) {
                Box(
                    modifier = Modifier
                        .width(dockedPlayerWidth(windowWidth))
                        .fillMaxHeight(),
                ) {
                    nowPlaying(currentSong, true)
                }
            }
        }

        // Fullscreen Now Playing Sheet when opened on phones/smaller screens
        val activePlayerSong = player.song
        if (showNowPlaying && activePlayerSong != null && !playerDocked) {
            ModalBottomSheet(
                onDismissRequest = { showNowPlaying = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                dragHandle = null,
                containerColor = MaterialTheme.colorScheme.background,
                modifier = Modifier.fillMaxSize(),
            ) {
                nowPlaying(activePlayerSong, false)
            }
        }

        // Action Sheets & Dialog Hosts
        songActions?.let { song ->
            SongActionsSheet(
                song = song,
                fromPlayer = menuFromPlayer,
                onDismiss = { songActions = null },
                onPlayNext = { playNext(song); songActions = null },
                onAddToQueue = { addToQueue(song); songActions = null },
                onStartRadio = { startRadio(song); songActions = null },
                onDownload = { downloadSong(song); songActions = null },
                onAddToPlaylist = {
                    playlistTarget = song
                    songActions = null
                },
                onOpenArtist = { id ->
                    songActions = null
                    showNowPlaying = false
                    viewModel.openDetail(id, song.artist, context.getString(R.string.artist), null, BrowseType.ARTIST)
                },
                onOpenAlbum = { id ->
                    songActions = null
                    showNowPlaying = false
                    viewModel.openDetail(id, song.albumName ?: song.title, song.artist, song.thumbnailUrl, BrowseType.ALBUM)
                },
            )
        }

        browseActions?.let { target ->
            BrowseActionsSheet(
                target = target,
                onDismiss = { browseActions = null },
                onPlay = {
                    withBrowseSongs(target) { songs -> play(songs, 0) }
                    browseActions = null
                },
                onShuffle = {
                    withBrowseSongs(target) { songs ->
                        QueueShuffle.enableForNextQueue()
                        play(songs, songs.indices.random())
                    }
                    browseActions = null
                },
                onPlayNext = {
                    withBrowseSongs(target) { songs -> playSongsNext(songs) }
                    browseActions = null
                },
                onAddToQueue = {
                    withBrowseSongs(target) { songs -> addSongsToQueue(songs) }
                    browseActions = null
                },
                onDownload = {
                    withBrowseSongs(target) { songs ->
                        val downloadTarget = target.browseId?.let { id ->
                            DownloadTarget(id, target.title, target.subtitle, target.thumbnailUrl, target.type)
                        }
                        startDownload(songs, downloadTarget)
                    }
                    browseActions = null
                },
            )
        }

        if (playlistTarget != null || creatingPlaylist) {
            PlaylistPickerSheet(
                targetSong = playlistTarget,
                playlists = playlists,
                loading = playlistsLoading,
                onDismiss = {
                    playlistTarget = null
                    creatingPlaylist = false
                },
                onCreatePlaylist = { title -> viewModel.createPlaylist(title, playlistTarget) },
                onAddToPlaylist = { playlistId ->
                    playlistTarget?.let { song -> viewModel.addToPlaylist(playlistId, song) }
                },
            )
        }

        if (showDownloadManager) {
            DownloadManagerSheet(onDismiss = { showDownloadManager = false })
        }

        if (showUpdateDialog && updateNotice != null) {
            UpdateAvailableDialog(
                updateInfo = updateNotice,
                onDismiss = { showUpdateDialog = false },
            )
        }

        if (showAppLanguage) {
            AppLanguageDialog(onDismiss = { showAppLanguage = false })
        }

        if (showLyricsSources) {
            LyricsSourcesDialog(onDismiss = { showLyricsSources = false })
        }

        if (showAccountSelector) {
            AccountProfileSelector(
                accounts = googleAccounts,
                activeAccountId = activeAccountId,
                activeProfileId = activeProfileId,
                onSelectAccount = { acc -> viewModel.selectAccount(acc) },
                onSelectProfile = { prof -> viewModel.selectProfile(prof) },
                onDismiss = { showAccountSelector = false },
            )
        }

        if (showListenBrainzLogin) {
            ListenBrainzTokenAlert(
                token = listenBrainzToken,
                onSave = { token -> AppSettings.setListenBrainzToken(token) },
                onDismiss = { showListenBrainzLogin = false },
            )
        }

        if (showLastfmLogin) {
            LastfmLoginAlert(
                onDismiss = { showLastfmLogin = false },
            )
        }

        editingSource?.let { source ->
            SourceEditorAlert(
                source = source,
                onDismiss = { editingSource = null },
                onSave = { updated -> SourceRegistry.updateSource(updated) },
            )
        }

        DiscordDialogHost(
            dialog = discordDialog,
            onDismiss = { discordDialog = null },
        )

        if (showDiscordLogin) {
            DiscordLoginScreen(onDismiss = { showDiscordLogin = false })
        }

        if (showSpotifyCanvasAuth) {
            SpotifyCanvasAuthScreen(onDismiss = { showSpotifyCanvasAuth = false })
        }

        webSession?.let { mode ->
            when (mode) {
                WebSessionMode.SIGN_IN -> YtMusicLoginScreen(
                    onDismiss = { webSession = null },
                    onSuccess = { viewModel.reloadAccount() },
                )
                WebSessionMode.DISCORD -> DiscordLoginScreen(
                    onDismiss = { webSession = null },
                )
            }
        }

        if (showReplayShare) {
            ReplayShareSheet(
                summary = replay.summary,
                holder = account?.name.orEmpty(),
                page = replaySharePage,
                onDismiss = { showReplayShare = false },
            )
        }

        replayStory?.let { storyPage ->
            ReplayStories(
                initialPage = storyPage,
                summary = replay.summary,
                holder = account?.name.orEmpty(),
                onDismiss = { replayStory = null },
                onShare = { page ->
                    replaySharePage = page
                    showReplayShare = true
                },
            )
        }
    }
}

/** Top bar scroll offset threshold for transitioning title displays on detail pages. */
private val DETAIL_TITLE_DROP = 120.dp
private const val SEEK_END_GUARD_MS = 1000L
private const val TAB_KEY = "tab_"
private const val TAB_HOME = 0
private const val TAB_EXPLORE = 1
private const val TAB_LIBRARY = 2
private const val TAB_SEARCH = 3

private fun String.isDeviceFolder(): Boolean = startsWith("local:")

private fun LibrarySort.localizedLabel(): String = when (this) {
    LibrarySort.RECENTLY_ADDED -> "Recently Added"
    LibrarySort.TITLE -> "Title"
    LibrarySort.ARTIST -> "Artist"
}

private fun SongSort.localizedLabel(): String = when (this) {
    SongSort.DEFAULT -> "Default"
    SongSort.TITLE -> "Title"
    SongSort.ARTIST -> "Artist"
    SongSort.ALBUM -> "Album"
}

private fun formatDurationText(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
}
