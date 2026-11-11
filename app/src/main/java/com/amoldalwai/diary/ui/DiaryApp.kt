package com.amoldalwai.diary.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.amoldalwai.diary.DiaryViewModel
import com.amoldalwai.diary.Screen
import com.amoldalwai.diary.data.BookStatus
import com.amoldalwai.diary.isHub

@Composable
fun DiaryApp(vm: DiaryViewModel) {
    BackHandler(enabled = !vm.locked && vm.screen !is Screen.Home && vm.screen !is Screen.Editor) { vm.goBack() }

    val habitUi = HabitUi(vm.habits, vm.settings.habitSound, vm::habitsDone, vm::loadHabitDay, vm::toggleHabit)
    val booksUi = BooksUi(
        enabled = vm.settings.booksEnabled,
        reading = vm.books.filter { it.status == BookStatus.Reading },
        pageOn = vm::bookPage,
        loadLogs = vm::loadBookDay,
        setPage = vm::setBookPage,
        addBook = vm::addBook,
        cover = vm::bookCover,
    )

    Box(Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = vm.screen,
            contentKey = { it::class },
            transitionSpec = { screenTransition(initialState, targetState) },
            label = "screens",
        ) { screen ->
            when (screen) {
                Screen.Home -> HomeScreen(
                    entries = vm.homeEntries,
                    loading = vm.homeLoading,
                    loadingMore = vm.homeLoadingMore,
                    hasMore = vm.homeHasMore,
                    stats = vm.homeStats,
                    weekDays = vm.weekDays,
                    onThisDay = vm.onThisDay,
                    imageFile = vm::imageFile,
                    onNewEntry = { vm.openNew() },
                    onOpenEntry = { vm.openDetail(it.id) },
                    onDayClick = vm::openDay,
                    onLoadMore = vm::loadMoreHome,
                    onOpenSearch = { vm.openHub(Screen.Search) },
                    onOpenCalendar = { vm.openHub(Screen.Calendar) },
                    onOpenInsights = { vm.openHub(Screen.Insights) },
                    onOpenSettings = { vm.openHub(Screen.Settings) },
                    onOpenAlbum = { vm.openHub(Screen.Album) },
                    habitUi = habitUi,
                    booksEnabled = vm.settings.booksEnabled,
                    onOpenBooks = { vm.openHub(Screen.Books) },
                    gamesEnabled = vm.settings.gamesEnabled,
                    onOpenGames = { vm.openHub(Screen.Games) },
                    watchEnabled = vm.settings.watchEnabled,
                    onOpenWatch = { vm.openHub(Screen.Watch) },
                )

                Screen.Watch -> WatchScreen(
                    items = vm.watchItems,
                    cover = vm::watchCover,
                    onAdd = { result, kind, status, season, episode -> vm.addWatch(result, kind, status, season, episode) {} },
                    onUpdate = vm::updateWatch,
                    onDelete = vm::deleteWatch,
                    onBack = vm::goBack,
                )

                Screen.Games -> GamesScreen(
                    games = vm.games,
                    cover = vm::gameCover,
                    onAddGame = { result, platform, status -> vm.addGame(result, platform, status) {} },
                    onUpdate = vm::updateGame,
                    onDelete = vm::deleteGame,
                    onBack = vm::goBack,
                )

                Screen.Habits -> HabitsScreen(
                    stats = vm.habitStats,
                    onAdd = vm::addHabit,
                    onUpdate = vm::updateHabit,
                    onDelete = vm::deleteHabit,
                    onBack = vm::goBack,
                )

                Screen.Books -> BooksScreen(
                    books = vm.books,
                    cover = vm::bookCover,
                    onAddBook = { result, status, total, current -> vm.addBook(result, status, total, current) {} },
                    onUpdate = vm::updateBook,
                    onDelete = vm::deleteBook,
                    onBack = vm::goBack,
                )

                Screen.Album -> AlbumScreen(
                    photos = vm.albumPhotos,
                    total = vm.albumTotal,
                    loading = vm.albumLoading,
                    hasMore = vm.albumHasMore,
                    imageFile = vm::imageFile,
                    onLoadMore = vm::loadMoreAlbum,
                    onOpenEntry = { vm.openDetail(it, from = Screen.Album) },
                    onBack = vm::goBack,
                )

                Screen.Calendar -> CalendarScreen(
                    month = vm.calendarMonth,
                    markers = vm.monthMarkers,
                    dayEntries = vm.dayEntries,
                    selected = vm.calendarSelected,
                    imageFile = vm::imageFile,
                    onMonthChange = vm::showMonth,
                    onSelect = vm::selectDay,
                    onBack = vm::goBack,
                    onOpenEntry = { vm.openDetail(it.id, from = Screen.Calendar) },
                    onNewEntry = { vm.openNew(it, from = Screen.Calendar) },
                    loadForExport = vm::entriesForExport,
                )

                Screen.Search -> SearchScreen(
                    results = vm.searchResults,
                    totalCount = vm.searchCount,
                    loading = vm.searchLoading,
                    hasMore = vm.searchHasMore,
                    knownTags = vm.knownTags,
                    imageFile = vm::imageFile,
                    query = vm.searchQuery,
                    onQuery = { vm.searchQuery = it },
                    mood = vm.searchMood,
                    onMood = { vm.searchMood = it },
                    tag = vm.searchTag,
                    onTag = { vm.searchTag = it },
                    onSearch = vm::runSearch,
                    onLoadMore = vm::loadMoreSearch,
                    onBack = vm::goBack,
                    onOpenEntry = { vm.openDetail(it.id, from = Screen.Search) },
                )

                Screen.Insights -> InsightsScreen(stats = vm.insights, habitStats = vm.habitStats, onBack = vm::goBack)

                Screen.Settings -> SettingsScreen(
                    settings = vm.settings,
                    imageFile = vm::imageFile,
                    loadForExport = vm::entriesForExport,
                    onBack = vm::goBack,
                    onSetReminder = vm::setReminder,
                    syncState = vm.syncState,
                    onConnectDrive = vm::connectDrive,
                    onConsentResult = vm::onConsentResult,
                    onSyncNow = vm::syncNow,
                    onDisconnectDrive = vm::disconnectDrive,
                    storage = vm.storage,
                    habitCount = vm.habits.size,
                    onOpenHabits = { vm.openHub(Screen.Habits, from = Screen.Settings) },
                    onOpenBooks = { vm.openHub(Screen.Books, from = Screen.Settings) },
                    onOpenGames = { vm.openHub(Screen.Games, from = Screen.Settings) },
                    onOpenWatch = { vm.openHub(Screen.Watch, from = Screen.Settings) },
                )

                is Screen.Detail -> PageTurn {
                    val entry = vm.detailEntry?.takeIf { it.id == screen.id }
                    if (entry != null) {
                        DetailScreen(
                            entry = entry,
                            imageFile = vm::imageFile,
                            onBack = vm::goBack,
                            onEdit = { vm.openEditor(entry) },
                            onDelete = { vm.delete(entry) },
                            onFlip = vm::flipDetail,
                            habitUi = habitUi,
                            booksUi = booksUi,
                        )
                    } else {
                        Box(Modifier.fillMaxSize())
                    }
                }

                is Screen.Editor -> EditorScreen(
                    original = screen.entry,
                    knownTags = vm.knownTags,
                    imageFile = vm::imageFile,
                    onPickImages = vm::addImages,
                    onAddDrawing = vm::addDrawing,
                    habitUi = habitUi,
                    booksUi = booksUi,
                    onSave = { vm.save(screen.entry, it, screen.isNew) },
                    onDiscard = { vm.discard(screen.entry, screen.isNew) },
                )
            }
        }

        AnimatedVisibility(
            visible = vm.locked,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(350)) + scaleOut(tween(350), targetScale = 1.08f),
        ) {
            LockScreen(
                biometricEnabled = vm.settings.biometricEnabled,
                verify = vm::unlockWithPin,
                onBiometricSuccess = vm::unlockWithBiometric,
            )
        }
    }
}

private fun screenTransition(from: Screen, to: Screen): ContentTransform = when {
    to is Screen.Editor ->
        (slideInVertically(tween(450, easing = FastOutSlowInEasing)) { it / 5 } + fadeIn(tween(350))) togetherWith
            (fadeOut(tween(250)) + scaleOut(tween(400), targetScale = 0.94f))

    from is Screen.Editor ->
        (fadeIn(tween(350)) + scaleIn(tween(400), initialScale = 0.96f)) togetherWith
            (slideOutVertically(tween(400, easing = FastOutSlowInEasing)) { it / 5 } + fadeOut(tween(300)))

    // The page turn itself is animated inside DetailScreen's PageTurn wrapper.
    to is Screen.Detail ->
        fadeIn(tween(150)) togetherWith fadeOut(tween(650))

    from is Screen.Detail ->
        ContentTransform(
            targetContentEnter = fadeIn(tween(350)),
            initialContentExit = fadeOut(tween(450, delayMillis = 250)),
            targetContentZIndex = -1f,
        )

    to.isHub() ->
        (slideInHorizontally(tween(400, easing = FastOutSlowInEasing)) { it / 4 } + fadeIn(tween(350))) togetherWith
            fadeOut(tween(250))

    from.isHub() ->
        fadeIn(tween(350)) togetherWith
            (slideOutHorizontally(tween(400, easing = FastOutSlowInEasing)) { it / 4 } + fadeOut(tween(300)))

    else ->
        fadeIn(tween(350)) togetherWith (fadeOut(tween(250)) + scaleOut(tween(350), targetScale = 1.06f))
}
