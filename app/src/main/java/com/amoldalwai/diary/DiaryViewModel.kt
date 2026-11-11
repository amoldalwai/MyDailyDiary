package com.amoldalwai.diary

import android.app.Application
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.amoldalwai.diary.data.DayMarker
import com.amoldalwai.diary.data.ActivityGroup
import com.amoldalwai.diary.data.Book
import com.amoldalwai.diary.data.BookResult
import com.amoldalwai.diary.data.BookStatus
import com.amoldalwai.diary.data.BooksRepository
import com.amoldalwai.diary.data.CoverStore
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
import com.amoldalwai.diary.data.HabitNudge
import com.amoldalwai.diary.reminder.HabitNudges
import com.amoldalwai.diary.data.HabitsRepository
import com.amoldalwai.diary.data.HomeHero
import com.amoldalwai.diary.data.OpenLibrary
import com.amoldalwai.diary.data.DiaryEntry
import com.amoldalwai.diary.data.DiaryRepository
import com.amoldalwai.diary.data.HomeStats
import com.amoldalwai.diary.data.InsightStats
import com.amoldalwai.diary.data.Mood
import com.amoldalwai.diary.data.PageCursor
import com.amoldalwai.diary.data.PhotoRef
import com.amoldalwai.diary.data.Progress
import com.amoldalwai.diary.data.ProgressStats
import com.amoldalwai.diary.data.LevelInfo
import com.amoldalwai.diary.data.SettingsStore
import com.amoldalwai.diary.data.StorageUsage
import com.amoldalwai.diary.reminder.ReminderScheduler
import com.amoldalwai.diary.sync.AuthResult
import com.amoldalwai.diary.sync.DriveAuth
import com.amoldalwai.diary.sync.DriveAuthException
import com.amoldalwai.diary.sync.DriveSync
import com.amoldalwai.diary.sync.SyncState
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
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
    data object Journal : Screen
    data object Library : Screen
    data object More : Screen
    data object Calendar : Screen
    data object Search : Screen
    data object Insights : Screen
    data object Settings : Screen
    data object Album : Screen
    data object Habits : Screen
    data object Books : Screen
    data object Games : Screen
    data object Watch : Screen
    data object Profile : Screen
    data object LevelPath : Screen
    data class Detail(val id: String) : Screen
    data class Editor(val entry: DiaryEntry, val isNew: Boolean) : Screen
}

fun Screen.isTab() = this is Screen.Home || this is Screen.Journal || this is Screen.Library || this is Screen.More

fun Screen.isHub() =
    this is Screen.Calendar || this is Screen.Search || this is Screen.Insights || this is Screen.Settings ||
        this is Screen.Album || this is Screen.Habits || this is Screen.Books || this is Screen.Games || this is Screen.Watch ||
        this is Screen.Profile || this is Screen.LevelPath

/** A one-off "you finished something" moment: confetti, a chime and a banner. */
data class Celebration(val token: Int, val message: String)

data class LevelUp(val level: Int, val title: String, val from: Int, val rankUp: Boolean)

/** The splash shown after a journal entry is saved. */
data class EntrySplash(val token: Int, val points: Int, val streak: Int, val total: Int)

class DiaryViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = DiaryRepository(app)
    private val habitsRepo = HabitsRepository(app)
    private val booksRepo = BooksRepository(app)
    private val gamesRepo = GamesRepository(app)
    private val watchRepo = WatchRepository(app)
    private val hero = HomeHero(app)
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
    private var currentTab: Screen = Screen.Home

    var quickAddOpen by mutableStateOf(false)
        private set
    var quickAddMood by mutableStateOf<Mood?>(null)
        private set

    // Points, level and awards are worked out from the data above; only the celebrated level is remembered.
    var progressStats by mutableStateOf(ProgressStats())
        private set
    var level by mutableStateOf(Progress.levelFor(0))
        private set
    var celebration by mutableStateOf<Celebration?>(null)
        private set
    var levelUp by mutableStateOf<LevelUp?>(null)
        private set
    var awardBanner by mutableStateOf<List<String>>(emptyList())
        private set
    var entrySplash by mutableStateOf<EntrySplash?>(null)
        private set
    var heroFile by mutableStateOf<File?>(null)
        private set
    var heroLoading by mutableStateOf(false)
        private set
    private var celebrationToken = 0
    private var progressReady = false

    init {
        viewModelScope.launch {
            heroFile = withContext(Dispatchers.IO) { hero.latest() }
            refreshHero()
        }
        if (!settings.coversShrunk) {
            viewModelScope.launch(Dispatchers.IO) {
                val freed = CoverStore.shrinkExisting(booksRepo.imageFile(""))
                settings.markCoversShrunk()
                Log.i("Covers", "Shrunk stored covers, freed $freed bytes")
            }
        }
        viewModelScope.launch {
            repo.migrateLegacy()
            reloadHome()
            reloadPersonal()
            progressReady = true
            refreshProgress()
            runSync()
        }
        viewModelScope.launch {
            snapshotFlow { listOf(books, games, watchItems, habits, homeStats, habitDone.values.sumOf { it.size }) }
                .collectLatest {
                    if (progressReady) {
                        delay(400)
                        refreshProgress()
                    }
                }
        }
    }

    private suspend fun refreshHero() {
        hero.refresh(LocalDate.now())?.let { heroFile = it }
    }

    fun refreshHeroNow() {
        if (heroLoading) return
        if (!hero.isOnline()) {
            Toast.makeText(getApplication(), "You're offline, so the photo stays as it is", Toast.LENGTH_SHORT).show()
            return
        }
        heroLoading = true
        viewModelScope.launch {
            val fresh = hero.refresh(LocalDate.now(), force = true)
            if (fresh != null) heroFile = fresh else Toast.makeText(getApplication(), "Couldn't load a new photo", Toast.LENGTH_SHORT).show()
            heroLoading = false
        }
    }

    private suspend fun refreshProgress() {
        val (entries, photos, streak) = repo.progressBasics()
        val b = books
        val g = games
        val w = watchItems
        val stats = ProgressStats(
            entries = entries,
            photos = photos,
            bestStreak = streak,
            perfectHabitDays = habitsRepo.perfectDays(),
            booksFinished = b.count { it.status == BookStatus.Finished },
            gamesCompleted = g.count { it.status == GameStatus.Completed },
            moviesWatched = w.count { it.kind == WatchKind.Movie && it.status == WatchStatus.Watched },
            seriesFinished = w.count { it.kind == WatchKind.Series && it.status == WatchStatus.Watched },
            ratings = b.count { it.rating != null } + g.count { it.rating != null } + w.count { it.rating != null },
            reviews = b.count { it.review.isNotBlank() } + g.count { it.review.isNotBlank() } + w.count { it.review.isNotBlank() },
            libraryItems = b.size + g.size + w.size,
        )
        val seen = settings.celebratedAwards
        val info = Progress.levelFor(Progress.points(stats), settings.celebratedLevel)
        val reached = Progress.reachedKeys(stats)
        progressStats = stats
        level = info
        if (seen == null) {
            // First run with this feature: existing data counts, but nothing is celebrated.
            settings.updateCelebrated(info.level, reached)
            return
        }
        val fresh = reached - seen
        val rose = info.level > settings.celebratedLevel
        if (!rose && fresh.isEmpty()) return
        val from = settings.celebratedLevel
        settings.updateCelebrated(info.level, seen + reached)
        if (fresh.isNotEmpty()) {
            awardBanner = fresh.mapNotNull { key ->
                val award = Progress.awards.firstOrNull { it.id == key.substringBefore(':') } ?: return@mapNotNull null
                val tier = com.amoldalwai.diary.data.AwardTier.entries[key.substringAfter(':').toInt()]
                "${award.emoji} ${award.name} · ${tier.label}"
            }
        }
        if (rose) levelUp = LevelUp(info.level, info.title, from, Progress.rankIndex(info.level) > Progress.rankIndex(from))
    }

    fun dismissLevelUp() {
        levelUp = null
    }

    fun dismissAwardBanner() {
        awardBanner = emptyList()
    }

    fun celebrate(message: String) {
        celebration = Celebration(++celebrationToken, message)
    }

    fun clearCelebration(token: Int) {
        if (celebration?.token == token) celebration = null
    }

    fun updateProfile(name: String, emoji: String, bio: String, banner: Int) = settings.updateProfile(name, emoji, bio, banner)

    fun profilePhotoFile(): File? = settings.profilePhoto?.let { repo.profileFile(it) }

    fun setProfilePhoto(uri: Uri) {
        viewModelScope.launch {
            val name = repo.importProfilePhoto(uri)
            if (name == null) {
                Toast.makeText(getApplication(), "Couldn't use that photo", Toast.LENGTH_SHORT).show()
                return@launch
            }
            settings.profilePhoto?.let { repo.deleteProfileFile(it) }
            settings.updateProfilePhoto(name)
        }
    }

    fun clearProfilePhoto() {
        settings.profilePhoto?.let { viewModelScope.launch { repo.deleteProfileFile(it) } }
        settings.updateProfilePhoto(null)
    }

    fun dismissEntrySplash() {
        entrySplash = null
    }

    private fun showEntrySplash(photos: Int) {
        entrySplash = EntrySplash(++celebrationToken, Progress.entryPoints(photos), homeStats.streak, homeStats.total)
    }

    fun imageFile(name: String): File = repo.imageFile(name)

    suspend fun entriesForExport(from: Long?, to: Long?): List<DiaryEntry> = repo.fullEntries(from, to)

    // ---- App lock ----

    fun onAppStopped() {
        stoppedAt = SystemClock.elapsedRealtime()
    }

    fun onAppStarted() {
        if (stoppedAt != 0L) {
            viewModelScope.launch { runSync() }
            viewModelScope.launch { refreshHero() }
            // A habit may have been ticked from a notification while the app was away.
            viewModelScope.launch { reloadPersonal() }
        }
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

    fun setNudges(enabled: Boolean, from: Int, to: Int, gap: Int) {
        settings.updateNudges(enabled, from, to, gap)
        applyNudges()
    }

    fun setHabitNudge(habitId: String, nudge: HabitNudge) {
        settings.updateHabitNudge(habitId, nudge)
        applyNudges()
    }

    private fun applyNudges() {
        val context = getApplication<Application>()
        if (settings.nudgesEnabled) HabitNudges.schedule(context, settings) else HabitNudges.cancel(context)
    }

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
        val nowDone = habitId !in habitDone[date.toEpochDay()].orEmpty()
        setHabit(habitId, date, nowDone)
        return nowDone
    }

    fun setHabit(habitId: String, date: LocalDate, done: Boolean) {
        val day = date.toEpochDay()
        val current = habitDone[day].orEmpty()
        habitDone[day] = if (done) current + habitId else current - habitId
        viewModelScope.launch {
            habitsRepo.setDone(habitId, day, done)
            if (screen == Screen.Habits) habitStats = habitsRepo.stats(LocalDate.now())
            scheduleSync()
        }
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

    // Two titles are the same when they only differ in case or spacing.
    private fun same(a: String, b: String) =
        a.trim().replace(Regex("\\s+"), " ").equals(b.trim().replace(Regex("\\s+"), " "), ignoreCase = true)

    // The item is already in the library: reuse it, so a journal page links to it instead of a copy.
    private fun alreadyAdded(title: String, id: String, onDone: (String) -> Unit) {
        Toast.makeText(getApplication(), "“$title” is already in your library", Toast.LENGTH_SHORT).show()
        onDone(id)
    }

    // ---- Books ----

    fun loadBookDay(date: LocalDate) {
        val day = date.toEpochDay()
        viewModelScope.launch { bookLogs[day] = booksRepo.logsOn(day) }
    }

    fun bookPage(book: Book, date: LocalDate): Int = bookLogs[date.toEpochDay()]?.get(book.id) ?: book.currentPage

    fun bookCover(name: String): File = booksRepo.imageFile(name)

    fun setBookPage(book: Book, date: LocalDate, page: Int) {
        val day = date.toEpochDay()
        viewModelScope.launch {
            val updated = booksRepo.logPage(book.id, day, page)
            bookLogs[day] = booksRepo.logsOn(day)
            books = booksRepo.books()
            if (updated != null && updated.status == BookStatus.Finished && book.status != BookStatus.Finished) {
                celebrate("Finished “${updated.title}”")
            }
            scheduleSync()
        }
    }

    fun addBook(result: BookResult, status: BookStatus, totalPages: Int?, currentPage: Int, onDone: (String) -> Unit) {
        viewModelScope.launch {
            val existing = books.firstOrNull {
                same(it.title, result.title) && (it.authors.isBlank() || result.authors.isBlank() || same(it.authors, result.authors))
            }
            if (existing != null) return@launch alreadyAdded(existing.title, existing.id, onDone)
            val cover = result.coverId?.let { OpenLibrary.downloadCover(it) }?.let { booksRepo.saveCover(it) }
            val now = System.currentTimeMillis()
            val book = Book(
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
            )
            booksRepo.save(book)
            books = booksRepo.books()
            if (status == BookStatus.Finished) celebrate("Finished “${book.title}”")
            onDone(book.id)
            scheduleSync()
        }
    }

    fun setBookStatus(id: String, status: BookStatus) {
        books.firstOrNull { it.id == id }?.let { updateBook(it.copy(status = status)) }
    }

    fun updateBook(book: Book) {
        val now = System.currentTimeMillis()
        val wasFinished = books.firstOrNull { it.id == book.id }?.status == BookStatus.Finished
        val fixed = when (book.status) {
            BookStatus.Want -> book.copy(startedAt = null, finishedAt = null)
            BookStatus.Reading -> book.copy(startedAt = book.startedAt ?: now, finishedAt = null)
            BookStatus.Finished -> book.copy(
                startedAt = book.startedAt ?: now,
                finishedAt = book.finishedAt ?: now,
                currentPage = book.totalPages ?: book.currentPage,
            )
        }
        if (!wasFinished && fixed.status == BookStatus.Finished) celebrate("Finished “${fixed.title}”")
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
    fun addGame(result: GameResult, platform: String, status: GameStatus, onDone: (String) -> Unit) {
        viewModelScope.launch {
            val existing = games.firstOrNull { same(it.title, result.title) }
            if (existing != null) return@launch alreadyAdded(existing.title, existing.id, onDone)
            val bytes = when {
                result.appId > 0 -> SteamStore.downloadCover(result.appId)
                result.coverUrl != null -> WikipediaGames.downloadCover(result.coverUrl)
                else -> null
            }
            val cover = bytes?.let { gamesRepo.saveCover(it) }
            val now = System.currentTimeMillis()
            val game = Game(
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
            )
            gamesRepo.save(game)
            games = gamesRepo.games()
            if (status == GameStatus.Completed) celebrate("Completed “${game.title}”")
            onDone(game.id)
            scheduleSync()
        }
    }

    fun setGameStatus(id: String, status: GameStatus) {
        games.firstOrNull { it.id == id }?.let { updateGame(it.copy(status = status)) }
    }

    fun updateGame(game: Game) {
        val now = System.currentTimeMillis()
        val wasDone = games.firstOrNull { it.id == game.id }?.status == GameStatus.Completed
        val fixed = when (game.status) {
            GameStatus.Backlog -> game.copy(startedAt = null, completedAt = null)
            GameStatus.Playing -> game.copy(startedAt = game.startedAt ?: now, completedAt = null)
            GameStatus.Completed -> game.copy(startedAt = game.startedAt ?: now, completedAt = game.completedAt ?: now)
        }
        if (!wasDone && fixed.status == GameStatus.Completed) celebrate("Completed “${fixed.title}”")
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

    fun addWatch(result: WatchResult, kind: WatchKind, status: WatchStatus, season: Int, episode: Int, onDone: (String) -> Unit) {
        viewModelScope.launch {
            val existing = watchItems.firstOrNull { it.kind == kind && same(it.title, result.title) }
            if (existing != null) return@launch alreadyAdded(existing.title, existing.id, onDone)
            val bytes = result.coverUrl?.let { WikipediaGames.downloadCover(it) }
            val cover = bytes?.let { watchRepo.saveCover(it) }
            val now = System.currentTimeMillis()
            val item = WatchItem(
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
            )
            watchRepo.save(item)
            watchItems = watchRepo.items()
            if (status == WatchStatus.Watched) celebrate("Watched “${item.title}”")
            onDone(item.id)
            scheduleSync()
        }
    }

    fun setWatchStatus(id: String, status: WatchStatus) {
        watchItems.firstOrNull { it.id == id }?.let { updateWatch(it.copy(status = status)) }
    }

    fun updateWatch(item: WatchItem) {
        val now = System.currentTimeMillis()
        val wasDone = watchItems.firstOrNull { it.id == item.id }?.status == WatchStatus.Watched
        // Starting an episode counter on a watchlist title means you've started watching it.
        val status = if (item.status == WatchStatus.Watchlist && (item.season > 0 || item.episode > 0)) WatchStatus.Watching else item.status
        val fixed = when (status) {
            WatchStatus.Watchlist -> item.copy(status = status, startedAt = null, finishedAt = null)
            WatchStatus.Watching -> item.copy(status = status, startedAt = item.startedAt ?: now, finishedAt = null)
            WatchStatus.Watched -> item.copy(status = status, startedAt = item.startedAt ?: now, finishedAt = item.finishedAt ?: now)
        }
        if (!wasDone && fixed.status == WatchStatus.Watched) celebrate("Watched “${fixed.title}”")
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
            origin = currentTab
            screen = hubReturn ?: currentTab
            hubReturn = null
            return
        }
        if (screen.isTab()) {
            if (screen != Screen.Home) openTab(Screen.Home)
            return
        }
        screen = origin
    }

    /** Bottom-bar destinations; they stay on screen as the place everything else returns to. */
    fun openTab(target: Screen) {
        currentTab = target
        origin = target
        hubReturn = null
        if (target == Screen.Library) viewModelScope.launch { albumTotal = repo.photoTotal() }
        if (target == Screen.Journal) {
            showMonth(calendarMonth)
            selectDay(calendarSelected)
        }
        screen = target
    }

    // ---- Quick add ----

    fun openQuickAdd(mood: Mood? = null) {
        quickAddMood = mood
        quickAddOpen = true
    }

    fun closeQuickAdd() {
        quickAddOpen = false
    }

    fun saveQuickEntry(mood: Mood?, tags: List<String>, note: String, links: List<String>) {
        quickAddOpen = false
        val entry = DiaryEntry(mood = mood, tags = tags, body = note.trim(), links = links)
        viewModelScope.launch {
            repo.save(entry)
            afterChange()
            showEntrySplash(0)
            scheduleSync()
        }
    }

    /** "Open full note": the page editor, started with whatever was already picked and typed. */
    fun quickToEditor(mood: Mood?, tags: List<String>, note: String, links: List<String>) {
        quickAddOpen = false
        openNew(prefill = DiaryEntry(mood = mood, tags = tags, body = note, links = links))
    }

    /** Attach or detach library items on a saved page. */
    fun updateLinks(entry: DiaryEntry, links: List<String>) {
        val updated = entry.copy(links = links, updatedAt = System.currentTimeMillis())
        detailEntry = updated
        viewModelScope.launch {
            repo.save(updated)
            afterChange()
            scheduleSync()
        }
    }

    fun updateActivityGroups(groups: List<ActivityGroup>) = settings.updateActivityGroups(groups)

    fun openHub(target: Screen, from: Screen? = null) {
        origin = currentTab
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
            Screen.Profile, Screen.LevelPath -> viewModelScope.launch { refreshProgress() }
            else -> Unit
        }
        screen = target
    }

    fun openNew(date: LocalDate = LocalDate.now(), from: Screen = currentTab, prefill: DiaryEntry? = null) {
        // No back-dating: a new page can only be for today or a coming day.
        if (date.isBefore(LocalDate.now())) {
            Toast.makeText(getApplication(), "Past days can't be added to. Write on today's page instead.", Toast.LENGTH_SHORT).show()
            return
        }
        origin = from
        screen = Screen.Editor((prefill ?: DiaryEntry()).copy(dateEpochDay = date.toEpochDay()), isNew = true)
    }

    fun openDay(date: LocalDate) {
        viewModelScope.launch {
            val existing = repo.entriesOn(date.toEpochDay()).firstOrNull()
            if (existing != null) openDetail(existing.id) else openNew(date)
        }
    }

    fun openDetail(id: String, from: Screen = currentTab) {
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
            if (isNew) showEntrySplash(updated.images.size)
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
