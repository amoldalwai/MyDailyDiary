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
import com.amoldalwai.diary.data.ExerciseData
import com.amoldalwai.diary.data.CaughtPokemon
import com.amoldalwai.diary.data.PokeApi
import com.amoldalwai.diary.data.PokemonMath
import com.amoldalwai.diary.data.PokemonRepository
import com.amoldalwai.diary.data.ExerciseMath
import com.amoldalwai.diary.data.ExerciseRepository
import com.amoldalwai.diary.data.Muscle
import com.amoldalwai.diary.data.FinishInfo
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.time.LocalDate
import kotlin.random.Random
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
    data object About : Screen
    data object Exercise : Screen
    data object PokemonTree : Screen
    data object Pokeball : Screen
    data class Detail(val id: String) : Screen
    data class Editor(val entry: DiaryEntry, val isNew: Boolean) : Screen
}

fun Screen.isTab() = this is Screen.Home || this is Screen.Journal || this is Screen.Library || this is Screen.More

fun Screen.isHub() =
    this is Screen.Calendar || this is Screen.Search || this is Screen.Insights || this is Screen.Settings ||
        this is Screen.Album || this is Screen.Habits || this is Screen.Books || this is Screen.Games || this is Screen.Watch ||
        this is Screen.Profile || this is Screen.LevelPath || this is Screen.About || this is Screen.Exercise ||
        this is Screen.PokemonTree || this is Screen.Pokeball

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
    private val exerciseRepo = ExerciseRepository(app)
    private val pokemonRepo = PokemonRepository(app)
    private val pendingImages = mutableSetOf<String>()

    val settings = SettingsStore(app)

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

    /** Per-day entry counts and moods for the last year, for the Insights charts. */
    var yearMarkers by mutableStateOf<Map<Long, DayMarker>>(emptyMap())
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

    /** Something was just marked finished and needs a rating before it is saved; [save] runs with the chosen rating. */
    class FinishRequest(val title: String, val noun: String, val startRating: Int?, val save: (Int) -> Unit)

    var finishRequest by mutableStateOf<FinishRequest?>(null)
        private set

    fun answerFinish(rating: Int?) {
        val request = finishRequest ?: return
        finishRequest = null
        if (rating != null) request.save(rating)
    }
    var levelUp by mutableStateOf<LevelUp?>(null)
        private set
    var awardBanner by mutableStateOf<List<String>>(emptyList())
        private set
    var entrySplash by mutableStateOf<EntrySplash?>(null)
        private set
    var heroFile by mutableStateOf<File?>(null)
        private set

    /** Which of the banner gradients shows when there is no photo; any number, the banner takes it modulo the count. */
    var heroGradient by mutableIntStateOf(Random.nextInt(1000))
        private set

    // First-run slides, and the step of the spotlight tour (null when it isn't running).
    var showWelcome by mutableStateOf(!settings.onboardingDone)
        private set
    var tourStep by mutableStateOf<Int?>(null)
        private set

    fun openWelcome() {
        showWelcome = true
    }

    fun finishWelcome(startTour: Boolean) {
        settings.markOnboardingDone()
        showWelcome = false
        if (startTour) startTour()
    }

    fun startTour() {
        quickAddOpen = false
        openTab(Screen.Home)
        tourStep = 0
    }

    fun goToTourStep(step: Int?) {
        tourStep = step
    }
    private var celebrationToken = 0
    private var progressReady = false

    init {
        viewModelScope.launch {
            // The banner used to download photos; clear what is left of them.
            withContext(Dispatchers.IO) { File(app.filesDir, "hero").deleteRecursively() }
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
        useOwnHero()
    }

    private var ownHeroName: String? = null

    /** Picks a random journal photo for the banner when that setting is on; otherwise the banner shows a gradient. */
    private suspend fun useOwnHero(): Boolean {
        val name = if (settings.heroOwnPhotos) repo.bannerPhoto(ownHeroName) else null
        ownHeroName = name
        heroFile = name?.let(repo::imageFile)
        return name != null
    }

    fun setHeroOwnPhotos(enabled: Boolean) {
        settings.updateHeroOwnPhotos(enabled)
        viewModelScope.launch {
            if (!useOwnHero() && enabled) {
                Toast.makeText(getApplication(), "Add a photo to a journal page and it can appear here", Toast.LENGTH_LONG).show()
            }
        }
    }

    /** The refresh icon: another random photo, or another gradient. */
    fun refreshHeroNow() {
        heroGradient += 1 + Random.nextInt(3)
        viewModelScope.launch { useOwnHero() }
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
        if (settings.pokemonEnabled) syncPokemon()
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
        if (rose) {
            levelUp = LevelUp(info.level, info.title, from, Progress.rankIndex(info.level) > Progress.rankIndex(from))
            if (settings.pokemonEnabled) settings.pokemonRevealLevel = info.level
        }
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
        if (stoppedAt != 0L) syncPokemon()
        if (stoppedAt != 0L) {
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
        }
    }

    fun addHabit(name: String, emoji: String) {
        viewModelScope.launch {
            habitsRepo.save(Habit(name = name.trim(), emoji = emoji, position = habits.size))
            reloadPersonal()
            loadHabitStats()
        }
    }

    fun updateHabit(habit: Habit, name: String, emoji: String) {
        viewModelScope.launch {
            habitsRepo.save(habit.copy(name = name.trim(), emoji = emoji, updatedAt = System.currentTimeMillis()))
            reloadPersonal()
            loadHabitStats()
        }
    }

    fun deleteHabit(habit: Habit) {
        viewModelScope.launch {
            habitsRepo.delete(habit.id)
            reloadPersonal()
            loadHabitStats()
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

    // ---- Pokémon ----

    var caught by mutableStateOf<List<CaughtPokemon>>(emptyList())
        private set
    var pokemonSyncing by mutableStateOf(false)
        private set

    /** Three Pokémon just unlocked by a level-up, waiting for the reveal. */
    var pokemonReveal by mutableStateOf<List<CaughtPokemon>?>(null)
        private set

    /** True when the reveal is a replay from the journey rather than a fresh level-up. */
    var pokemonReplay by mutableStateOf(false)
        private set

    /** The level tree shows pages of five levels; these are the first and last page on screen. */
    var treeMinPage by mutableIntStateOf(0)
        private set
    var treeMaxPage by mutableIntStateOf(0)
        private set
    val treeNames = mutableStateMapOf<Int, String>()
    var treeLoading by mutableStateOf(false)
        private set
    var treeOffline by mutableStateOf(false)
        private set
    private var pokemonSyncJob: Job? = null

    fun pokemonImage(id: Int) = pokemonRepo.imageFile(id)

    fun loadPokemon() {
        viewModelScope.launch { caught = pokemonRepo.caught() }
        syncPokemon()
    }

    /** Downloads the Pokémon the current level has unlocked and not fetched yet; stops quietly when offline and tries again later. */
    fun syncPokemon() {
        if (!settings.pokemonEnabled || pokemonSyncJob?.isActive == true) return
        pokemonSyncJob = viewModelScope.launch {
            pokemonRepo.removeSounds()
            val upTo = (level.level * PokemonMath.PER_LEVEL).coerceAtMost(PokemonMath.total)
            val have = pokemonRepo.caughtIds()
            val missing = (1..upTo).filter { it !in have }
            if (missing.isNotEmpty() && PokeApi.isOnline(getApplication())) {
                pokemonSyncing = true
                var failures = 0
                for (id in missing) {
                    var saved = false
                    repeat(2) { attempt ->
                        if (!saved) {
                            saved = runCatching {
                                val (pokemon, picture) = PokeApi.fetch(id, PokemonMath.levelOf(id))
                                pokemonRepo.save(pokemon, picture)
                            }.onFailure { Log.w("Pokemon", "Download of #$id failed (try ${attempt + 1})", it) }.isSuccess
                            if (!saved) delay(1500)
                        }
                    }
                    // Three misses in a row means the network is the problem; the next sync tries again.
                    failures = if (saved) 0 else failures + 1
                    if (failures >= 3) break
                    caught = pokemonRepo.caught()
                }
                pokemonSyncing = false
            }
            caught = pokemonRepo.caught()
            revealWhenReady()
        }
    }

    private fun revealWhenReady() {
        val reveal = settings.pokemonRevealLevel
        if (reveal == 0 || pokemonReveal != null) return
        val ids = PokemonMath.idsFor(reveal).toSet()
        val found = caught.filter { it.id in ids }.sortedBy { it.id }
        if (found.size == ids.size) {
            pokemonReveal = found
        } else if (!PokeApi.isOnline(getApplication())) {
            Toast.makeText(getApplication(), "New Pokémon are waiting. Connect to the internet to catch them.", Toast.LENGTH_LONG).show()
            settings.pokemonRevealLevel = 0
        }
    }


    fun dismissPokemonReveal(openPokeball: Boolean) {
        pokemonReveal = null
        if (!pokemonReplay) settings.pokemonRevealLevel = 0
        pokemonReplay = false
        if (openPokeball) openHub(Screen.Pokeball)
    }

    /** Plays the unlock animation again for a level whose Pokémon are all caught. */
    fun replayPokemon(level: Int) {
        val ids = PokemonMath.idsFor(level).toSet()
        val found = caught.filter { it.id in ids }.sortedBy { it.id }
        if (found.size != ids.size || pokemonReveal != null) return
        pokemonReplay = true
        pokemonReveal = found
    }

    fun setPokemonEnabled(enabled: Boolean) {
        settings.updatePokemonEnabled(enabled)
        if (enabled) syncPokemon()
    }

    private fun openPokemonTree() {
        treeNames.putAll(settings.pokemonNames)
        treeMinPage = PokemonMath.pageOf(level.level)
        treeMaxPage = treeMinPage
        loadPokemon()
        retryTree()
    }

    fun retryTree() {
        viewModelScope.launch {
            treeLoading = true
            treeOffline = false
            for (page in treeMinPage..treeMaxPage) loadTreeNames(page)
            treeLoading = false
        }
        syncPokemon()
    }

    fun treeMore(direction: Int) {
        val page = if (direction < 0 && treeMinPage > 0) --treeMinPage else if (direction > 0 && treeMaxPage < PokemonMath.pages - 1) ++treeMaxPage else return
        viewModelScope.launch {
            treeLoading = true
            treeOffline = false
            loadTreeNames(page)
            treeLoading = false
        }
    }

    private suspend fun loadTreeNames(page: Int) {
        val ids = PokemonMath.levelsOnPage(page).flatMap { PokemonMath.idsFor(it) }
        if (ids.all { it in treeNames }) return
        if (!PokeApi.isOnline(getApplication())) {
            treeOffline = true
            return
        }
        runCatching { PokeApi.names(ids.first() - 1, ids.size) }
            .onSuccess {
                treeNames.putAll(it)
                settings.pokemonNames = treeNames.toMap()
            }
            .onFailure { treeOffline = true }
    }

    // ---- Exercise ----

    var exercise by mutableStateOf(ExerciseData())
        private set

    fun loadExercise() {
        viewModelScope.launch {
            if (!settings.exerciseSeeded) {
                settings.exerciseSeeded = true
                ExerciseMath.defaults.forEachIndexed { i, (name, icon, target) ->
                    exerciseRepo.save(Muscle(name = name, icon = icon, target = target, position = i))
                }
            }
            exercise = exerciseRepo.load()
        }
    }

    /** Only today can be changed; earlier days are a record. */
    fun toggleMuscle(id: String) {
        val today = LocalDate.now().toEpochDay()
        val trained = (id to today) in exercise.logs
        viewModelScope.launch {
            exerciseRepo.setTrained(id, today, !trained)
            exercise = exerciseRepo.load()
        }
    }

    fun toggleRestToday() {
        val today = LocalDate.now().toEpochDay()
        val rest = today in exercise.restDays
        if (!rest && exercise.logs.any { it.second == today }) return
        viewModelScope.launch {
            exerciseRepo.setRest(today, !rest)
            exercise = exerciseRepo.load()
        }
    }

    fun addMuscle(name: String, icon: String, target: Int) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        if (exercise.muscles.any { it.name.equals(clean, ignoreCase = true) }) {
            Toast.makeText(getApplication(), "$clean is already in your list", Toast.LENGTH_SHORT).show()
            return
        }
        viewModelScope.launch {
            if (!exerciseRepo.restore(clean)) {
                val next = (exercise.muscles.maxOfOrNull { it.position } ?: -1) + 1
                exerciseRepo.save(Muscle(name = clean, icon = icon, target = target, position = next))
            }
            exercise = exerciseRepo.load()
        }
    }

    fun updateMuscle(muscle: Muscle) {
        viewModelScope.launch {
            exerciseRepo.save(muscle)
            exercise = exerciseRepo.load()
        }
    }

    fun removeMuscle(id: String) {
        viewModelScope.launch {
            exerciseRepo.remove(id)
            exercise = exerciseRepo.load()
        }
    }

    fun setWeeklyGoal(days: Int) = settings.updateWeeklyGoal(days)

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
            val total = updated?.totalPages
            if (updated != null && updated.status != BookStatus.Finished && total != null && updated.currentPage >= total) {
                updateBook(updated.copy(status = BookStatus.Finished))
            }
        }
    }

    fun addBook(result: BookResult, status: BookStatus, totalPages: Int?, currentPage: Int, finish: FinishInfo?, onDone: (String) -> Unit) {
        viewModelScope.launch {
            val existing = books.firstOrNull {
                same(it.title, result.title) && (it.authors.isBlank() || result.authors.isBlank() || same(it.authors, result.authors))
            }
            if (existing != null) return@launch alreadyAdded(existing.title, existing.id, onDone)
            val cover = result.coverId?.let { OpenLibrary.downloadCover(it) }?.let { booksRepo.saveCover(it) }
            val now = System.currentTimeMillis()
            val done = status == BookStatus.Finished
            val book = Book(
                openLibraryKey = result.key,
                title = result.title,
                authors = result.authors,
                year = result.year,
                cover = cover,
                totalPages = totalPages ?: result.pages,
                currentPage = if (done) (totalPages ?: result.pages ?: currentPage) else currentPage,
                status = status,
                startedAt = if (status == BookStatus.Reading) now else null,
                finishedAt = if (done && finish == null) now else null,
                finishedYear = if (done) finish?.year else null,
                rating = if (done) finish?.rating else null,
                updatedAt = now,
            )
            booksRepo.save(book)
            books = booksRepo.books()
            if (done && finish?.year == LocalDate.now().year) celebrate("Finished “${book.title}”")
            onDone(book.id)
        }
    }

    fun setBookStatus(id: String, status: BookStatus) {
        books.firstOrNull { it.id == id }?.let { updateBook(it.copy(status = status)) }
    }

    fun updateBook(book: Book) {
        val now = System.currentTimeMillis()
        val newlyDone = book.status == BookStatus.Finished && books.firstOrNull { it.id == book.id }?.status != BookStatus.Finished
        // Finishing needs a rating; nothing is saved until one is given.
        if (newlyDone && book.rating == null) {
            finishRequest = FinishRequest(book.title, "book", 3) { rating -> updateBook(book.copy(rating = rating)) }
            return
        }
        val fixed = when (book.status) {
            BookStatus.Want -> book.copy(startedAt = null, finishedAt = null, finishedYear = null)
            BookStatus.Reading -> book.copy(startedAt = book.startedAt ?: now, finishedAt = null, finishedYear = null)
            BookStatus.Finished -> book.copy(
                startedAt = book.startedAt ?: now,
                finishedAt = if (newlyDone) now else book.finishedAt,
                finishedYear = if (newlyDone) null else book.finishedYear,
                currentPage = book.totalPages ?: book.currentPage,
            )
        }
        if (newlyDone) celebrate("Finished “${fixed.title}”")
        viewModelScope.launch {
            booksRepo.save(fixed.copy(updatedAt = now))
            books = booksRepo.books()
        }
    }

    fun deleteBook(book: Book) {
        viewModelScope.launch {
            booksRepo.delete(book.id)
            books = booksRepo.books()
        }
    }

    // ---- Games ----

    fun gameCover(name: String): File = gamesRepo.imageFile(name)

    /** A result with neither a Steam id nor a cover URL is a hand-typed game and gets no cover. */
    fun addGame(result: GameResult, platform: String, status: GameStatus, finish: FinishInfo?, onDone: (String) -> Unit) {
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
            val done = status == GameStatus.Completed
            val game = Game(
                steamAppId = result.appId.takeIf { it > 0 },
                title = result.title,
                developer = result.developer,
                year = result.year,
                platform = platform,
                cover = cover,
                status = status,
                startedAt = if (status == GameStatus.Playing) now else null,
                completedAt = if (done && finish == null) now else null,
                completedYear = if (done) finish?.year else null,
                rating = if (done) finish?.rating else null,
                updatedAt = now,
            )
            gamesRepo.save(game)
            games = gamesRepo.games()
            if (done && finish?.year == LocalDate.now().year) celebrate("Completed “${game.title}”")
            onDone(game.id)
        }
    }

    fun setGameStatus(id: String, status: GameStatus) {
        games.firstOrNull { it.id == id }?.let { updateGame(it.copy(status = status)) }
    }

    fun updateGame(game: Game) {
        val now = System.currentTimeMillis()
        val newlyDone = game.status == GameStatus.Completed && games.firstOrNull { it.id == game.id }?.status != GameStatus.Completed
        if (newlyDone && game.rating == null) {
            finishRequest = FinishRequest(game.title, "game", null) { rating -> updateGame(game.copy(rating = rating)) }
            return
        }
        val fixed = when (game.status) {
            GameStatus.Backlog -> game.copy(startedAt = null, completedAt = null, completedYear = null)
            GameStatus.Playing -> game.copy(startedAt = game.startedAt ?: now, completedAt = null, completedYear = null)
            GameStatus.Completed -> game.copy(
                startedAt = game.startedAt ?: now,
                completedAt = if (newlyDone) now else game.completedAt,
                completedYear = if (newlyDone) null else game.completedYear,
            )
        }
        if (newlyDone) celebrate("Completed “${fixed.title}”")
        viewModelScope.launch {
            gamesRepo.save(fixed.copy(updatedAt = now))
            games = gamesRepo.games()
        }
    }

    fun deleteGame(game: Game) {
        viewModelScope.launch {
            gamesRepo.delete(game.id)
            games = gamesRepo.games()
        }
    }

    // ---- Movies and series ----

    fun watchCover(name: String): File = watchRepo.imageFile(name)

    fun addWatch(result: WatchResult, kind: WatchKind, status: WatchStatus, season: Int, episode: Int, finish: FinishInfo?, onDone: (String) -> Unit) {
        viewModelScope.launch {
            val existing = watchItems.firstOrNull { it.kind == kind && same(it.title, result.title) }
            if (existing != null) return@launch alreadyAdded(existing.title, existing.id, onDone)
            val bytes = result.coverUrl?.let { WikipediaGames.downloadCover(it) }
            val cover = bytes?.let { watchRepo.saveCover(it) }
            val now = System.currentTimeMillis()
            val done = status == WatchStatus.Watched
            val item = WatchItem(
                kind = kind,
                title = result.title,
                year = result.year,
                info = result.info,
                cover = cover,
                status = status,
                season = if (kind == WatchKind.Series) season else 0,
                episode = if (kind == WatchKind.Series) episode else 0,
                startedAt = if (status == WatchStatus.Watching) now else null,
                finishedAt = if (done && finish == null) now else null,
                finishedYear = if (done) finish?.year else null,
                rating = if (done) finish?.rating else null,
                updatedAt = now,
            )
            watchRepo.save(item)
            watchItems = watchRepo.items()
            if (done && finish?.year == LocalDate.now().year) celebrate("Watched “${item.title}”")
            onDone(item.id)
        }
    }

    fun setWatchStatus(id: String, status: WatchStatus) {
        watchItems.firstOrNull { it.id == id }?.let { updateWatch(it.copy(status = status)) }
    }

    fun updateWatch(item: WatchItem) {
        val now = System.currentTimeMillis()
        val newlyDone = item.status == WatchStatus.Watched && watchItems.firstOrNull { it.id == item.id }?.status != WatchStatus.Watched
        if (newlyDone && item.rating == null) {
            finishRequest = FinishRequest(item.title, if (item.kind == WatchKind.Series) "series" else "movie", null) { rating -> updateWatch(item.copy(rating = rating)) }
            return
        }
        // Starting an episode counter on a watchlist title means you've started watching it.
        val status = if (item.status == WatchStatus.Watchlist && (item.season > 0 || item.episode > 0)) WatchStatus.Watching else item.status
        val fixed = when (status) {
            WatchStatus.Watchlist -> item.copy(status = status, startedAt = null, finishedAt = null, finishedYear = null)
            WatchStatus.Watching -> item.copy(status = status, startedAt = item.startedAt ?: now, finishedAt = null, finishedYear = null)
            WatchStatus.Watched -> item.copy(
                status = status,
                startedAt = item.startedAt ?: now,
                finishedAt = if (newlyDone) now else item.finishedAt,
                finishedYear = if (newlyDone) null else item.finishedYear,
            )
        }
        if (newlyDone) celebrate("Watched “${fixed.title}”")
        viewModelScope.launch {
            watchRepo.save(fixed.copy(updatedAt = now))
            watchItems = watchRepo.items()
        }
    }

    fun deleteWatch(item: WatchItem) {
        viewModelScope.launch {
            watchRepo.delete(item.id)
            watchItems = watchRepo.items()
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
        if (note.isBlank()) return
        quickAddOpen = false
        val entry = DiaryEntry(mood = mood, tags = tags, body = note.trim(), links = links, style = settings.defaultPageStyle)
        viewModelScope.launch {
            repo.save(entry)
            afterChange()
            showEntrySplash(0)
        }
    }

    /** "Open full note": the page editor, started with whatever was already picked and typed. */
    fun quickToEditor(mood: Mood?, tags: List<String>, note: String, links: List<String>) {
        quickAddOpen = false
        openNew(prefill = DiaryEntry(mood = mood, tags = tags, body = note, links = links))
    }

    /** Attach or detach library items on a saved page. */
    fun updateLinks(entry: DiaryEntry, links: List<String>) {
        if (entry.date.isBefore(LocalDate.now())) return
        val updated = entry.copy(links = links, updatedAt = System.currentTimeMillis())
        detailEntry = updated
        viewModelScope.launch {
            repo.save(updated)
            afterChange()
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
                viewModelScope.launch {
                    val today = LocalDate.now()
                    yearMarkers = repo.monthMarkers(today.minusDays(370).toEpochDay(), today.toEpochDay())
                    insights = repo.insightStats(today)
                }
                loadHabitStats()
            }
            Screen.Search -> runSearch()
            Screen.Album -> loadAlbum()
            Screen.Settings -> loadStorage()
            Screen.Habits -> loadHabitStats()
            Screen.Exercise -> loadExercise()
            Screen.PokemonTree -> openPokemonTree()
            Screen.Pokeball -> loadPokemon()
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
        screen = Screen.Editor((prefill ?: DiaryEntry()).copy(dateEpochDay = date.toEpochDay(), style = settings.defaultPageStyle), isNew = true)
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
        // Past pages are a record: they can be read and deleted, not rewritten.
        if (entry.date.isBefore(LocalDate.now())) return
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
        }
    }

    fun deleteEntries(entries: List<DiaryEntry>) {
        if (entries.isEmpty()) return
        viewModelScope.launch {
            entries.forEach { repo.delete(it) }
            afterChange()
            Toast.makeText(getApplication(), if (entries.size == 1) "Page deleted" else "${entries.size} pages deleted", Toast.LENGTH_SHORT).show()
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
