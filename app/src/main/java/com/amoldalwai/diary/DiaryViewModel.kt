package com.amoldalwai.diary

import android.app.Application
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.SystemClock
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.amoldalwai.diary.data.DayMarker
import com.amoldalwai.diary.data.Book
import com.amoldalwai.diary.data.BookResult
import com.amoldalwai.diary.data.BookStatus
import com.amoldalwai.diary.data.BooksRepository
import com.amoldalwai.diary.data.Habit
import com.amoldalwai.diary.data.Game
import com.amoldalwai.diary.data.GameResult
import com.amoldalwai.diary.data.GameStatus
import com.amoldalwai.diary.data.GamesRepository
import com.amoldalwai.diary.data.SteamStore
import com.amoldalwai.diary.data.WikipediaGames
import com.amoldalwai.diary.data.WatchItem
import com.amoldalwai.diary.data.WatchKind
import com.amoldalwai.diary.data.WatchRepository
import com.amoldalwai.diary.data.WatchResult
import com.amoldalwai.diary.data.WatchStatus
import com.amoldalwai.diary.data.HabitStat
import com.amoldalwai.diary.data.HabitsRepository
import com.amoldalwai.diary.data.OpenLibrary
import com.amoldalwai.diary.data.DiaryEntry
import com.amoldalwai.diary.data.DiaryRepository
import com.amoldalwai.diary.data.HomeStats
import com.amoldalwai.diary.data.InsightStats
import com.amoldalwai.diary.data.Mood
import com.amoldalwai.diary.data.PageCursor
import com.amoldalwai.diary.data.PhotoRef
import com.amoldalwai.diary.data.SettingsStore
import com.amoldalwai.diary.data.StorageUsage
import com.amoldalwai.diary.reminder.ReminderScheduler
import com.amoldalwai.diary.sync.AuthResult
import com.amoldalwai.diary.sync.DriveAuth
import com.amoldalwai.diary.sync.DriveAuthException
import com.amoldalwai.diary.sync.DriveSync
import com.amoldalwai.diary.sync.SyncState
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max

sealed interface Screen {
    data object Home : Screen
    data object Calendar : Screen
    data object Search : Screen
    data object Insights : Screen
    data object Settings : Screen
    data object Album : Screen
    data object Habits : Screen
    data object Books : Screen
    data object Games : Screen
    data object Watch : Screen
    data class Detail(val id: String) : Screen
    data class Editor(val entry: DiaryEntry, val isNew: Boolean) : Screen
}

fun Screen.isHub() =
    this is Screen.Calendar || this is Screen.Search || this is Screen.Insights || this is Screen.Settings ||
        this is Screen.Album || this is Screen.Habits || this is Screen.Books || this is Screen.Games || this is Screen.Watch

class DiaryViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = DiaryRepository(app)
    private val habitsRepo = HabitsRepository(app)
    private val booksRepo = BooksRepository(app)
    private val gamesRepo = GamesRepository(app)
    private val watchRepo = WatchRepository(app)
    private val pendingImages = mutableSetOf<String>()

    val settings = SettingsStore(app)

    private val driveAuth = DriveAuth(app)
    private val driveSync = DriveSync(repo, habitsRepo, booksRepo, gamesRepo, watchRepo)
    private val syncMutex = Mutex()
    private var resyncRequested = false
    private var syncDebounce: Job? = null

    var syncState by mutableStateOf<SyncState>(SyncState.Idle)
        private set

    var screen by mutableStateOf<Screen>(Screen.Home)
        private set

    // Home: one page at a time, plus small aggregate queries for the header widgets.
    var homeEntries by mutableStateOf<List<DiaryEntry>>(emptyList())
        private set
    var homeLoading by mutableStateOf(true)
        private set
    var homeLoadingMore by mutableStateOf(false)
        private set
    var homeHasMore by mutableStateOf(true)
        private set
    var homeStats by mutableStateOf(HomeStats())
        private set
    var weekDays by mutableStateOf<Set<Long>>(emptySet())
        private set
    var onThisDay by mutableStateOf<List<DiaryEntry>>(emptyList())
        private set
    var knownTags by mutableStateOf<List<String>>(emptyList())
        private set

    // Calendar: markers for one month and the entries of one day.
    var calendarMonth by mutableStateOf(YearMonth.now())
        private set
    var calendarSelected by mutableStateOf(LocalDate.now())
        private set
    var monthMarkers by mutableStateOf<Map<Long, DayMarker>>(emptyMap())
        private set
    var dayEntries by mutableStateOf<List<DiaryEntry>>(emptyList())
        private set

    // Search
    var searchQuery by mutableStateOf("")
    var searchMood by mutableStateOf<Mood?>(null)
    var searchTag by mutableStateOf<String?>(null)
    var searchResults by mutableStateOf<List<DiaryEntry>>(emptyList())
        private set
    var searchCount by mutableIntStateOf(0)
        private set
    var searchLoading by mutableStateOf(false)
        private set
    var searchHasMore by mutableStateOf(false)
        private set
    private var searchJob: Job? = null

    var insights by mutableStateOf<InsightStats?>(null)
        private set

    // Stamp album: photos of a few entries at a time, newest first.
    var albumPhotos by mutableStateOf<List<PhotoRef>>(emptyList())
        private set
    var albumTotal by mutableIntStateOf(0)
        private set
    var albumLoading by mutableStateOf(false)
        private set
    var albumHasMore by mutableStateOf(false)
        private set
    private var albumCursor: PageCursor? = null

    var storage by mutableStateOf<StorageUsage?>(null)
        private set

    // Personalize: daily habits and the book shelf.
    var habits by mutableStateOf<List<Habit>>(emptyList())
        private set
    val habitDone = mutableStateMapOf<Long, Set<String>>()
    var habitStats by mutableStateOf<List<HabitStat>>(emptyList())
        private set
    var books by mutableStateOf<List<Book>>(emptyList())
        private set
    val bookLogs = mutableStateMapOf<Long, Map<String, Int>>()
    var games by mutableStateOf<List<Game>>(emptyList())
        private set
    var watchItems by mutableStateOf<List<WatchItem>>(emptyList())
        private set
    var detailEntry by mutableStateOf<DiaryEntry?>(null)
        private set

    var locked by mutableStateOf(settings.pinEnabled)
        private set
    private var stoppedAt = 0L

    // Where Detail/Editor return to: Home or the hub screen they were opened from.
    private var origin: Screen = Screen.Home
    private var hubReturn: Screen? = null

    init {
        viewModelScope.launch {
            repo.migrateLegacy()
            reloadHome()
            reloadPersonal()
            runSync()
        }
    }

    fun imageFile(name: String): File = repo.imageFile(name)

    suspend fun entriesForExport(from: Long?, to: Long?): List<DiaryEntry> = repo.fullEntries(from, to)

    // ---- App lock ----

    fun onAppStopped() {
        stoppedAt = SystemClock.elapsedRealtime()
    }

    fun onAppStarted() {
        if (stoppedAt != 0L) viewModelScope.launch { runSync() }
        if (settings.pinEnabled && stoppedAt != 0L && SystemClock.elapsedRealtime() - stoppedAt > LOCK_DELAY_MS) {
            locked = true
        }
    }

    fun unlockWithPin(pin: String): Boolean {
        val ok = settings.verifyPin(pin)
        if (ok) locked = false
        return ok
    }

    fun unlockWithBiometric() {
        locked = false
    }

    // ---- Settings ----

    fun setReminder(enabled: Boolean, hour: Int, minute: Int) {
        settings.updateReminder(enabled, hour, minute)
        val context = getApplication<Application>()
        if (enabled) {
            ReminderScheduler.schedule(context, hour, minute)
            val label = LocalTime.of(hour, minute).format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))
            Toast.makeText(context, "Daily reminder set for $label", Toast.LENGTH_SHORT).show()
        } else {
            ReminderScheduler.cancel(context)
        }
    }

    // ---- Home ----

    private suspend fun reloadHome() {
        val keep = max(homeEntries.size, PAGE_SIZE)
        val page = repo.page(null, keep)
        homeEntries = page
        homeHasMore = page.size >= keep
        homeLoading = false

        val today = LocalDate.now()
        homeStats = repo.homeStats(today)
        weekDays = repo.daysWithEntries(today.minusDays(6).toEpochDay(), today.toEpochDay())
        onThisDay = repo.onThisDay(today)
        knownTags = repo.tagCounts(30).map { it.first }
    }

    fun loadMoreHome() {
        if (homeLoading || homeLoadingMore || !homeHasMore) return
        val last = homeEntries.lastOrNull() ?: return
        homeLoadingMore = true
        viewModelScope.launch {
            val more = repo.page(PageCursor(last.dateEpochDay, last.createdAt), PAGE_SIZE)
            val known = homeEntries.mapTo(HashSet()) { it.id }
            homeEntries = homeEntries + more.filter { it.id !in known }
            homeHasMore = more.size >= PAGE_SIZE
            homeLoadingMore = false
        }
    }

    // ---- Calendar ----

    fun showMonth(month: YearMonth) {
        calendarMonth = month
        viewModelScope.launch {
            val markers = repo.monthMarkers(month.atDay(1).toEpochDay(), month.atEndOfMonth().toEpochDay())
            if (calendarMonth == month) monthMarkers = markers
        }
    }

    fun selectDay(date: LocalDate) {
        calendarSelected = date
        viewModelScope.launch {
            val entries = repo.entriesOn(date.toEpochDay())
            if (calendarSelected == date) dayEntries = entries
        }
    }

    // ---- Search ----

    fun runSearch() {
        searchJob?.cancel()
        val tokens = searchTokens()
        val mood = searchMood
        val tag = searchTag
        searchJob = viewModelScope.launch {
            searchLoading = true
            val results = repo.search(tokens, mood, tag, null, SEARCH_PAGE)
            searchResults = results
            searchHasMore = results.size >= SEARCH_PAGE
            searchCount = repo.searchCount(tokens, mood, tag)
            searchLoading = false
        }
    }

    fun loadMoreSearch() {
        if (searchLoading || !searchHasMore) return
        val last = searchResults.lastOrNull() ?: return
        val tokens = searchTokens()
        val mood = searchMood
        val tag = searchTag
        searchLoading = true
        searchJob = viewModelScope.launch {
            val more = repo.search(tokens, mood, tag, PageCursor(last.dateEpochDay, last.createdAt), SEARCH_PAGE)
            searchResults = searchResults + more
            searchHasMore = more.size >= SEARCH_PAGE
            searchLoading = false
        }
    }

    // ---- Habits ----

    private suspend fun reloadPersonal() {
        habits = habitsRepo.habits()
        books = booksRepo.books()
        games = gamesRepo.games()
        watchItems = watchRepo.items()
        habitDone.keys.toList().forEach { habitDone[it] = habitsRepo.doneIds(it) }
        bookLogs.keys.toList().forEach { bookLogs[it] = booksRepo.logsOn(it) }
    }

    fun loadHabitDay(date: LocalDate) {
        val day = date.toEpochDay()
        viewModelScope.launch { habitDone[day] = habitsRepo.doneIds(day) }
    }

    fun habitsDone(date: LocalDate): Set<String> = habitDone[date.toEpochDay()].orEmpty()

    /** Flips a habit for a day and returns the new state so the caller can celebrate a tick. */
    fun toggleHabit(habitId: String, date: LocalDate): Boolean {
        val day = date.toEpochDay()
        val current = habitDone[day].orEmpty()
        val nowDone = habitId !in current
        habitDone[day] = if (nowDone) current + habitId else current - habitId
        viewModelScope.launch {
            habitsRepo.setDone(habitId, day, nowDone)
            scheduleSync()
        }
        return nowDone
    }

    fun addHabit(name: String, emoji: String) {
        viewModelScope.launch {
            habitsRepo.save(Habit(name = name.trim(), emoji = emoji, position = habits.size))
            reloadPersonal()
            loadHabitStats()
            scheduleSync()
        }
    }

    fun updateHabit(habit: Habit, name: String, emoji: String) {
        viewModelScope.launch {
            habitsRepo.save(habit.copy(name = name.trim(), emoji = emoji, updatedAt = System.currentTimeMillis()))
            reloadPersonal()
            loadHabitStats()
            scheduleSync()
        }
    }

    fun deleteHabit(habit: Habit) {
        viewModelScope.launch {
            habitsRepo.delete(habit.id)
            reloadPersonal()
            loadHabitStats()
            scheduleSync()
        }
    }

    fun loadHabitStats() {
        viewModelScope.launch { habitStats = habitsRepo.stats(LocalDate.now()) }
    }

    // ---- Books ----

    fun loadBookDay(date: LocalDate) {
        val day = date.toEpochDay()
        viewModelScope.launch { bookLogs[day] = booksRepo.logsOn(day) }
    }

    fun bookPage(book: Book, date: LocalDate): Int = bookLogs[date.toEpochDay()]?.get(book.id) ?: book.currentPage

    fun bookCover(name: String): File = booksRepo.imageFile(name)

    fun setBookPage(book: Book, date: LocalDate, page: Int, onFinished: (Book) -> Unit) {
        val day = date.toEpochDay()
        viewModelScope.launch {
            val updated = booksRepo.logPage(book.id, day, page)
            bookLogs[day] = booksRepo.logsOn(day)
            books = booksRepo.books()
            if (updated != null && updated.status == BookStatus.Finished && book.status != BookStatus.Finished) onFinished(updated)
            scheduleSync()
        }
    }

    fun addBook(result: BookResult, status: BookStatus, totalPages: Int?, currentPage: Int, onDone: () -> Unit) {
        viewModelScope.launch {
            val cover = result.coverId?.let { OpenLibrary.downloadCover(it) }?.let { booksRepo.saveCover(it) }
            val now = System.currentTimeMillis()
            booksRepo.save(
                Book(
                    openLibraryKey = result.key,
                    title = result.title,
                    authors = result.authors,
                    year = result.year,
                    cover = cover,
                    totalPages = totalPages ?: result.pages,
                    currentPage = if (status == BookStatus.Finished) (totalPages ?: result.pages ?: currentPage) else currentPage,
                    status = status,
                    startedAt = if (status == BookStatus.Want) null else now,
                    finishedAt = if (status == BookStatus.Finished) now else null,
                    updatedAt = now,
                ),
            )
            books = booksRepo.books()
            onDone()
            scheduleSync()
        }
    }

    fun updateBook(book: Book) {
        val now = System.currentTimeMillis()
        val fixed = when (book.status) {
            BookStatus.Want -> book.copy(startedAt = null, finishedAt = null)
            BookStatus.Reading -> book.copy(startedAt = book.startedAt ?: now, finishedAt = null)
            BookStatus.Finished -> book.copy(
                startedAt = book.startedAt ?: now,
                finishedAt = book.finishedAt ?: now,
                currentPage = book.totalPages ?: book.currentPage,
            )
        }
        viewModelScope.launch {
            booksRepo.save(fixed.copy(updatedAt = now))
            books = booksRepo.books()
            scheduleSync()
        }
    }

    fun deleteBook(book: Book) {
        viewModelScope.launch {
            booksRepo.delete(book.id)
            books = booksRepo.books()
            scheduleSync()
        }
    }

    // ---- Games ----

    fun gameCover(name: String): File = gamesRepo.imageFile(name)

    /** A result with neither a Steam id nor a cover URL is a hand-typed game and gets no cover. */
    fun addGame(result: GameResult, platform: String, status: GameStatus, onDone: () -> Unit) {
        viewModelScope.launch {
            val bytes = when {
                result.appId > 0 -> SteamStore.downloadCover(result.appId)
                result.coverUrl != null -> WikipediaGames.downloadCover(result.coverUrl)
                else -> null
            }
            val cover = bytes?.let { gamesRepo.saveCover(it) }
            val now = System.currentTimeMillis()
            gamesRepo.save(
                Game(
                    steamAppId = result.appId.takeIf { it > 0 },
                    title = result.title,
                    developer = result.developer,
                    year = result.year,
                    platform = platform,
                    cover = cover,
                    status = status,
                    startedAt = if (status == GameStatus.Backlog) null else now,
                    completedAt = if (status == GameStatus.Completed) now else null,
                    updatedAt = now,
                ),
            )
            games = gamesRepo.games()
            onDone()
            scheduleSync()
        }
    }

    fun updateGame(game: Game) {
        val now = System.currentTimeMillis()
        val fixed = when (game.status) {
            GameStatus.Backlog -> game.copy(startedAt = null, completedAt = null)
            GameStatus.Playing -> game.copy(startedAt = game.startedAt ?: now, completedAt = null)
            GameStatus.Completed -> game.copy(startedAt = game.startedAt ?: now, completedAt = game.completedAt ?: now)
        }
        viewModelScope.launch {
            gamesRepo.save(fixed.copy(updatedAt = now))
            games = gamesRepo.games()
            scheduleSync()
        }
    }

    fun deleteGame(game: Game) {
        viewModelScope.launch {
            gamesRepo.delete(game.id)
            games = gamesRepo.games()
            scheduleSync()
        }
    }

    // ---- Movies and series ----

    fun watchCover(name: String): File = watchRepo.imageFile(name)

    fun addWatch(result: WatchResult, kind: WatchKind, status: WatchStatus, season: Int, episode: Int, onDone: () -> Unit) {
        viewModelScope.launch {
            val bytes = result.coverUrl?.let { WikipediaGames.downloadCover(it) }
            val cover = bytes?.let { watchRepo.saveCover(it) }
            val now = System.currentTimeMillis()
            watchRepo.save(
                WatchItem(
                    kind = kind,
                    title = result.title,
                    year = result.year,
                    info = result.info,
                    cover = cover,
                    status = status,
                    season = if (kind == WatchKind.Series) season else 0,
                    episode = if (kind == WatchKind.Series) episode else 0,
                    startedAt = if (status == WatchStatus.Watchlist) null else now,
                    finishedAt = if (status == WatchStatus.Watched) now else null,
                    updatedAt = now,
                ),
            )
            watchItems = watchRepo.items()
            onDone()
            scheduleSync()
        }
    }

    fun updateWatch(item: WatchItem) {
        val now = System.currentTimeMillis()
        // Starting an episode counter on a watchlist title means you've started watching it.
        val status = if (item.status == WatchStatus.Watchlist && (item.season > 0 || item.episode > 0)) WatchStatus.Watching else item.status
        val fixed = when (status) {
            WatchStatus.Watchlist -> item.copy(status = status, startedAt = null, finishedAt = null)
            WatchStatus.Watching -> item.copy(status = status, startedAt = item.startedAt ?: now, finishedAt = null)
            WatchStatus.Watched -> item.copy(status = status, startedAt = item.startedAt ?: now, finishedAt = item.finishedAt ?: now)
        }
        viewModelScope.launch {
            watchRepo.save(fixed.copy(updatedAt = now))
            watchItems = watchRepo.items()
            scheduleSync()
        }
    }

    fun deleteWatch(item: WatchItem) {
        viewModelScope.launch {
            watchRepo.delete(item.id)
            watchItems = watchRepo.items()
            scheduleSync()
        }
    }

    // ---- Stamp album and storage ----

    private fun loadAlbum() {
        albumCursor = null
        albumPhotos = emptyList()
        albumHasMore = true
        albumLoading = true
        viewModelScope.launch {
            val page = repo.photoPage(null, ALBUM_PAGE)
            albumPhotos = page.photos
            albumCursor = page.next
            albumHasMore = page.hasMore
            albumTotal = repo.photoTotal()
            albumLoading = false
        }
    }

    fun loadMoreAlbum() {
        val cursor = albumCursor ?: return
        if (albumLoading || !albumHasMore) return
        albumLoading = true
        viewModelScope.launch {
            val page = repo.photoPage(cursor, ALBUM_PAGE)
            albumPhotos = albumPhotos + page.photos
            albumCursor = page.next
            albumHasMore = page.hasMore
            albumLoading = false
        }
    }

    private fun loadStorage() {
        viewModelScope.launch { storage = repo.storageUsage() }
    }

    private fun searchTokens() = searchQuery.lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }

    // ---- Navigation ----

    fun goBack() {
        if (screen.isHub()) {
            origin = Screen.Home
            screen = hubReturn ?: Screen.Home
            hubReturn = null
            return
        }
        screen = origin
    }

    fun openHub(target: Screen, from: Screen? = null) {
        origin = Screen.Home
        hubReturn = from
        when (target) {
            Screen.Calendar -> {
                showMonth(calendarMonth)
                selectDay(calendarSelected)
            }
            Screen.Insights -> {
                insights = null
                viewModelScope.launch { insights = repo.insightStats(LocalDate.now()) }
                loadHabitStats()
            }
            Screen.Search -> runSearch()
            Screen.Album -> loadAlbum()
            Screen.Settings -> loadStorage()
            Screen.Habits -> loadHabitStats()
            Screen.Books -> viewModelScope.launch { books = booksRepo.books() }
            Screen.Games -> viewModelScope.launch { games = gamesRepo.games() }
            Screen.Watch -> viewModelScope.launch { watchItems = watchRepo.items() }
            else -> Unit
        }
        screen = target
    }

    fun openNew(date: LocalDate = LocalDate.now(), from: Screen = Screen.Home) {
        origin = from
        screen = Screen.Editor(DiaryEntry(dateEpochDay = date.toEpochDay()), isNew = true)
    }

    fun openDay(date: LocalDate) {
        viewModelScope.launch {
            val existing = repo.entriesOn(date.toEpochDay()).firstOrNull()
            if (existing != null) openDetail(existing.id) else openNew(date)
        }
    }

    fun openDetail(id: String, from: Screen = Screen.Home) {
        viewModelScope.launch {
            val entry = repo.getEntry(id) ?: return@launch
            origin = from
            detailEntry = entry
            screen = Screen.Detail(id)
        }
    }

    fun openEditor(entry: DiaryEntry) {
        screen = Screen.Editor(entry, isNew = false)
    }

    /** Swipe between entries: the next remembered day ([newer]) or the previous one. */
    fun flipDetail(newer: Boolean, onNone: () -> Unit) {
        val current = detailEntry ?: return
        viewModelScope.launch {
            val next = repo.neighbor(current, newer)
            if (next == null) {
                onNone()
            } else {
                detailEntry = next
                screen = Screen.Detail(next.id)
            }
        }
    }

    // ---- Entries ----

    fun addImages(uris: List<Uri>, onDone: (List<String>) -> Unit) {
        viewModelScope.launch {
            val names = uris.mapNotNull { repo.importImage(it) }
            pendingImages += names
            onDone(names)
        }
    }

    fun addDrawing(bitmap: Bitmap, onDone: (String) -> Unit) {
        viewModelScope.launch {
            val name = repo.saveBitmap(bitmap)
            bitmap.recycle()
            pendingImages += name
            onDone(name)
        }
    }

    fun save(original: DiaryEntry, updated: DiaryEntry, isNew: Boolean) {
        val orphans = (original.images + pendingImages) - updated.images.toSet()
        pendingImages.clear()
        detailEntry = updated
        screen = if (isNew) origin else Screen.Detail(updated.id)
        viewModelScope.launch {
            repo.save(updated)
            repo.deleteImages(orphans)
            afterChange()
            scheduleSync()
        }
    }

    fun discard(original: DiaryEntry, isNew: Boolean) {
        val orphans = pendingImages - original.images.toSet()
        pendingImages.clear()
        screen = if (isNew) origin else Screen.Detail(original.id)
        viewModelScope.launch { repo.deleteImages(orphans) }
    }

    fun delete(entry: DiaryEntry) {
        detailEntry = null
        screen = origin
        viewModelScope.launch {
            repo.delete(entry)
            afterChange()
            scheduleSync()
        }
    }

    // ---- Google Drive sync ----

    fun connectDrive(onConsent: (PendingIntent) -> Unit) {
        viewModelScope.launch {
            try {
                when (val result = driveAuth.authorize()) {
                    is AuthResult.Token -> completeConnect(result.accessToken)
                    is AuthResult.NeedsConsent -> onConsent(result.pendingIntent)
                }
            } catch (e: Exception) {
                syncState = SyncState.Error(e.message ?: "Couldn't connect to Google")
            }
        }
    }

    fun onConsentResult(data: Intent?) {
        viewModelScope.launch {
            try {
                completeConnect(driveAuth.tokenFromIntent(data))
            } catch (e: Exception) {
                syncState = SyncState.Error("Google Drive permission wasn't granted")
            }
        }
    }

    fun syncNow() {
        viewModelScope.launch { runSync() }
    }

    fun disconnectDrive() {
        viewModelScope.launch {
            settings.driveEmail?.let { email -> runCatching { driveAuth.revoke(email) } }
            settings.updateDrive(null)
            syncState = SyncState.Idle
        }
    }

    private suspend fun completeConnect(token: String) {
        settings.updateDrive(driveAuth.userEmail(token) ?: "Google account")
        runSync()
    }

    private fun scheduleSync() {
        if (!settings.driveConnected) return
        syncDebounce?.cancel()
        syncDebounce = viewModelScope.launch {
            delay(3_000)
            runSync()
        }
    }

    private suspend fun runSync() {
        if (!settings.driveConnected) return
        if (!syncMutex.tryLock()) {
            resyncRequested = true
            return
        }
        try {
            // A started sync always finishes, even if a newer edit cancels the debounce job.
            withContext(NonCancellable) {
                do {
                    resyncRequested = false
                    syncOnce()
                } while (resyncRequested)
            }
        } finally {
            syncMutex.unlock()
        }
    }

    private suspend fun syncOnce() {
        syncState = SyncState.Syncing
        try {
            var token = (driveAuth.authorize() as? AuthResult.Token)?.accessToken
            if (token == null) {
                syncState = SyncState.NeedsReconnect
                return
            }
            val summary = try {
                driveSync.run(token)
            } catch (e: DriveAuthException) {
                driveAuth.clearToken(token)
                token = (driveAuth.authorize() as? AuthResult.Token)?.accessToken
                if (token == null) {
                    syncState = SyncState.NeedsReconnect
                    return
                }
                driveSync.run(token)
            }
            settings.updateLastSync(System.currentTimeMillis())
            syncState = SyncState.Idle
            if (summary.changedLocal) {
                afterChange()
                reloadPersonal()
            }
        } catch (e: Exception) {
            syncState = SyncState.Error(
                if (e is IOException) "Couldn't reach Google Drive. Check your connection." else e.message ?: "Sync failed",
            )
        }
    }

    private suspend fun afterChange() {
        reloadHome()
        monthMarkers = repo.monthMarkers(calendarMonth.atDay(1).toEpochDay(), calendarMonth.atEndOfMonth().toEpochDay())
        dayEntries = repo.entriesOn(calendarSelected.toEpochDay())
        insights = null
        storage = null
        if (origin == Screen.Album) loadAlbum()
        if (searchResults.isNotEmpty() || searchQuery.isNotBlank() || searchMood != null || searchTag != null) runSearch()
    }

    private companion object {
        const val LOCK_DELAY_MS = 30_000L
        const val PAGE_SIZE = 15
        const val SEARCH_PAGE = 20
        const val ALBUM_PAGE = 24
    }
}
