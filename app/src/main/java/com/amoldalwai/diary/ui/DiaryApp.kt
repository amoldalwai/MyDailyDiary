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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import com.amoldalwai.diary.DiaryViewModel
import com.amoldalwai.diary.Screen
import com.amoldalwai.diary.data.BookStatus
import com.amoldalwai.diary.data.GameStatus
import com.amoldalwai.diary.data.WatchStatus
import java.io.File
import java.time.Instant
import java.time.ZoneId
import com.amoldalwai.diary.isHub
import com.amoldalwai.diary.isTab

@Composable
fun DiaryApp(vm: DiaryViewModel) {
    BackHandler(enabled = !vm.locked && vm.screen !is Screen.Home && vm.screen !is Screen.Editor) { vm.goBack() }

    val habitUi = HabitUi(vm.habits, vm.settings.habitSound, vm::habitsDone, vm::loadHabitDay, vm::toggleHabit)
    val mediaUi = MediaUi(
        booksEnabled = vm.settings.booksEnabled,
        gamesEnabled = vm.settings.gamesEnabled,
        watchEnabled = vm.settings.watchEnabled,
        books = vm.books,
        games = vm.games,
        watch = vm.watchItems,
        bookCover = vm::bookCover,
        gameCover = vm::gameCover,
        watchCover = vm::watchCover,
        pageOn = vm::bookPage,
        loadLogs = vm::loadBookDay,
        setPage = vm::setBookPage,
        addBook = vm::addBook,
        addGame = vm::addGame,
        addWatch = vm::addWatch,
        setBookStatus = vm::setBookStatus,
        setGameStatus = vm::setGameStatus,
        setWatchStatus = vm::setWatchStatus,
    )

    val completions = remember(vm.books, vm.games, vm.watchItems) { completionPosters(vm) }
    val profilePhoto = vm.profilePhotoFile()

    Box(Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = vm.screen,
            contentKey = { it::class },
            transitionSpec = { screenTransition(initialState, targetState) },
            label = "screens",
        ) { screen ->
            when (screen) {
                Screen.Home -> HomeScreen(
                    recent = vm.homeEntries.take(3),
                    loading = vm.homeLoading,
                    onThisDay = vm.onThisDay,
                    onOpenEntry = { vm.openDetail(it.id) },
                    onSeeAll = { vm.openTab(Screen.Journal) },
                    onMood = { vm.openQuickAdd(it) },
                    habitUi = habitUi,
                    profileEmoji = vm.settings.profileEmoji,
                    profileName = vm.settings.profileName,
                    profilePhoto = profilePhoto,
                    heroImage = vm.heroFile.takeUnless { vm.heroUnavailable },
                    heroLoading = vm.heroLoading,
                    onRefreshHero = vm::refreshHeroNow,
                    imageFile = vm::imageFile,
                    level = vm.level,
                    onProfile = { vm.openHub(Screen.Profile) },
                )

                Screen.Journal -> JournalScreen(
                    entries = vm.homeEntries,
                    loading = vm.homeLoading,
                    loadingMore = vm.homeLoadingMore,
                    hasMore = vm.homeHasMore,
                    imageFile = vm::imageFile,
                    onOpenEntry = { vm.openDetail(it.id) },
                    onLoadMore = vm::loadMoreHome,
                    onSearch = { vm.openHub(Screen.Search) },
                    month = vm.calendarMonth,
                    markers = vm.monthMarkers,
                    dayEntries = vm.dayEntries,
                    selected = vm.calendarSelected,
                    onMonthChange = vm::showMonth,
                    onSelect = vm::selectDay,
                    onNewEntry = { vm.openNew(it) },
                    loadForExport = vm::entriesForExport,
                    completions = completions,
                    onDeleteEntries = vm::deleteEntries,
                )

                Screen.Library -> LibraryScreen(
                    books = vm.books,
                    games = vm.games,
                    watch = vm.watchItems,
                    albumTotal = vm.albumTotal,
                    booksEnabled = vm.settings.booksEnabled,
                    gamesEnabled = vm.settings.gamesEnabled,
                    watchEnabled = vm.settings.watchEnabled,
                    bookCover = vm::bookCover,
                    gameCover = vm::gameCover,
                    watchCover = vm::watchCover,
                    onOpenBooks = { vm.openHub(Screen.Books) },
                    onOpenGames = { vm.openHub(Screen.Games) },
                    onOpenWatch = { vm.openHub(Screen.Watch) },
                    onOpenAlbum = { vm.openHub(Screen.Album) },
                    onUpdateBook = vm::updateBook,
                    onDeleteBook = vm::deleteBook,
                    onUpdateGame = vm::updateGame,
                    onDeleteGame = vm::deleteGame,
                    onUpdateWatch = vm::updateWatch,
                    onDeleteWatch = vm::deleteWatch,
                )

                Screen.More -> MoreScreen(
                    entryCount = vm.homeStats.total,
                    onInsights = { vm.openHub(Screen.Insights) },
                    onHabits = { vm.openHub(Screen.Habits) },
                    onExercise = { vm.openHub(Screen.Exercise) },
                    onSettings = { vm.openHub(Screen.Settings) },
                    onAbout = { vm.openHub(Screen.About) },
                    profileName = vm.settings.profileName,
                    profileEmoji = vm.settings.profileEmoji,
                    profilePhoto = profilePhoto,
                    level = vm.level,
                    onProfile = { vm.openHub(Screen.Profile) },
                )

                Screen.Profile -> ProfileScreen(
                    name = vm.settings.profileName,
                    emoji = vm.settings.profileEmoji,
                    photo = profilePhoto,
                    bio = vm.settings.profileBio,
                    bannerIndex = vm.settings.profileBanner,
                    level = vm.level,
                    stats = vm.progressStats,
                    celebratedAwards = vm.settings.celebratedAwards.orEmpty(),
                    onSave = vm::updateProfile,
                    onPickPhoto = vm::setProfilePhoto,
                    onClearPhoto = vm::clearProfilePhoto,
                    onJourney = { vm.openHub(Screen.LevelPath, from = Screen.Profile) },
                    onBack = vm::goBack,
                )

                Screen.About -> AboutScreen(onBack = vm::goBack)

                Screen.Exercise -> ExerciseScreen(
                    data = vm.exercise,
                    weeklyGoal = vm.settings.weeklyGoal,
                    onToggleMuscle = vm::toggleMuscle,
                    onToggleRest = vm::toggleRestToday,
                    onAddMuscle = vm::addMuscle,
                    onUpdateMuscle = vm::updateMuscle,
                    onRemoveMuscle = vm::removeMuscle,
                    onGoal = vm::setWeeklyGoal,
                    onBack = vm::goBack,
                )

                Screen.LevelPath -> LevelPathScreen(
                    level = vm.level,
                    emoji = vm.settings.profileEmoji,
                    photo = profilePhoto,
                    onBack = vm::goBack,
                )

                Screen.Watch -> WatchScreen(
                    items = vm.watchItems,
                    cover = vm::watchCover,
                    onAdd = { result, kind, status, season, episode, finish -> vm.addWatch(result, kind, status, season, episode, finish) {} },
                    onUpdate = vm::updateWatch,
                    onDelete = vm::deleteWatch,
                    onBack = vm::goBack,
                )

                Screen.Games -> GamesScreen(
                    games = vm.games,
                    cover = vm::gameCover,
                    onAddGame = { result, platform, status, finish -> vm.addGame(result, platform, status, finish) {} },
                    onUpdate = vm::updateGame,
                    onDelete = vm::deleteGame,
                    onBack = vm::goBack,
                )

                Screen.Habits -> HabitsScreen(
                    stats = vm.habitStats,
                    onAdd = vm::addHabit,
                    onUpdate = vm::updateHabit,
                    onDelete = vm::deleteHabit,
                    onToggleToday = { id, done -> vm.setHabit(id, java.time.LocalDate.now(), done) },
                    onBack = vm::goBack,
                )

                Screen.Books -> BooksScreen(
                    books = vm.books,
                    cover = vm::bookCover,
                    onAddBook = { result, status, total, current, finish -> vm.addBook(result, status, total, current, finish) {} },
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
                    completions = completions,
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

                Screen.Insights -> InsightsScreen(
                    stats = vm.insights,
                    yearMarkers = vm.yearMarkers,
                    habitStats = vm.habitStats,
                    library = vm.progressStats,
                    level = vm.level,
                    onBack = vm::goBack,
                )

                Screen.Settings -> SettingsScreen(
                    settings = vm.settings,
                    imageFile = vm::imageFile,
                    loadForExport = vm::entriesForExport,
                    onBack = vm::goBack,
                    onSetReminder = vm::setReminder,
                    storage = vm.storage,
                    habitCount = vm.habits.size,
                    habits = vm.habits,
                    onNudges = vm::setNudges,
                    onHabitNudge = vm::setHabitNudge,
                    onOpenHabits = { vm.openHub(Screen.Habits, from = Screen.Settings) },
                    onOpenBooks = { vm.openHub(Screen.Books, from = Screen.Settings) },
                    onOpenGames = { vm.openHub(Screen.Games, from = Screen.Settings) },
                    onOpenWatch = { vm.openHub(Screen.Watch, from = Screen.Settings) },
                    onStartTour = vm::startTour,
                    onShowWelcome = vm::openWelcome,
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
                            mediaUi = mediaUi,
                            onLinks = { vm.updateLinks(entry, it) },
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
                    mediaUi = mediaUi,
                    onSave = { vm.save(screen.entry, it, screen.isNew) },
                    onDiscard = { vm.discard(screen.entry, screen.isNew) },
                )
            }
        }

        AnimatedVisibility(
            visible = vm.screen.isTab() && !vm.locked,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(tween(350, easing = FastOutSlowInEasing)) { it } + fadeIn(tween(250)),
            exit = slideOutVertically(tween(250)) { it } + fadeOut(tween(200)),
        ) {
            FloatingNavBar(current = vm.screen, onTab = vm::openTab, onAdd = { vm.openQuickAdd() })
        }

        if (vm.quickAddOpen && !vm.locked) {
            QuickAddSheet(
                groups = vm.settings.activityGroups,
                activitiesEnabled = vm.settings.quickActivities,
                habitUi = habitUi,
                mediaUi = mediaUi,
                initialMood = vm.quickAddMood,
                onDismiss = vm::closeQuickAdd,
                onSave = vm::saveQuickEntry,
                onFullNote = vm::quickToEditor,
                onGroupsChange = vm::updateActivityGroups,
            )
        }

        if (!vm.locked) {
            vm.finishRequest?.let { request ->
                FinishDialog(
                    title = request.title,
                    noun = request.noun,
                    startRating = request.startRating,
                    askYear = false,
                    onConfirm = { vm.answerFinish(it.rating) },
                    onCancel = { vm.answerFinish(null) },
                )
            }
            CompletionCelebration(vm.celebration, vm.settings.habitSound, vm::clearCelebration)
            if (vm.levelUp == null && vm.entrySplash == null) AwardBanner(vm.awardBanner, vm::dismissAwardBanner)
            EntrySavedSplash(vm.entrySplash, vm.settings.habitSound, vm::dismissEntrySplash)
            if (vm.entrySplash == null) LevelUpOverlay(vm.levelUp, vm.settings.habitSound, vm::dismissLevelUp)
            vm.tourStep?.let { step ->
                AppTourOverlay(
                    step = step,
                    onStep = vm::goToTourStep,
                    onStop = { vm.goToTourStep(null) },
                    onTry = { action ->
                        vm.goToTourStep(null)
                        when (action) {
                            TourAction.AddEntry -> vm.openQuickAdd()
                            TourAction.AddMovie -> vm.openHub(Screen.Watch)
                            TourAction.AddHabit -> vm.openHub(Screen.Habits)
                            TourAction.OpenExercise -> vm.openHub(Screen.Exercise)
                        }
                    },
                )
            }
            if (vm.showWelcome) {
                BackHandler { vm.finishWelcome(false) }
                WelcomeScreen(onFinish = vm::finishWelcome)
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

private fun completionPosters(vm: DiaryViewModel): Map<Long, List<File>> {
    val zone = ZoneId.systemDefault()
    fun day(millis: Long) = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate().toEpochDay()
    val map = HashMap<Long, MutableList<File>>()
    fun add(millis: Long?, cover: String?, file: (String) -> File) {
        if (millis != null && cover != null) map.getOrPut(day(millis)) { mutableListOf() }.add(file(cover))
    }
    vm.books.filter { it.status == BookStatus.Finished }.forEach { add(it.finishedAt, it.cover, vm::bookCover) }
    vm.games.filter { it.status == GameStatus.Completed }.forEach { add(it.completedAt, it.cover, vm::gameCover) }
    vm.watchItems.filter { it.status == WatchStatus.Watched }.forEach { add(it.finishedAt, it.cover, vm::watchCover) }
    return map
}

private fun screenTransition(from: Screen, to: Screen): ContentTransform = when {
    from.isTab() && to.isTab() -> fadeIn(tween(260)) togetherWith fadeOut(tween(180))

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
