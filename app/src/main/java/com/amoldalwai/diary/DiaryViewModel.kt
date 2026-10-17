package com.amoldalwai.diary

import android.app.Application
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.amoldalwai.diary.data.DayMarker
import com.amoldalwai.diary.data.DiaryEntry
import com.amoldalwai.diary.data.DiaryRepository
import com.amoldalwai.diary.data.HomeStats
import com.amoldalwai.diary.data.InsightStats
import com.amoldalwai.diary.data.Mood
import com.amoldalwai.diary.data.PageCursor
import com.amoldalwai.diary.data.SettingsStore
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
import java.time.YearMonth
import kotlin.math.max

sealed interface Screen {
    data object Home : Screen
    data object Calendar : Screen
    data object Search : Screen
    data object Insights : Screen
    data object Settings : Screen
    data class Detail(val id: String) : Screen
    data class Editor(val entry: DiaryEntry, val isNew: Boolean) : Screen
}

fun Screen.isHub() = this is Screen.Calendar || this is Screen.Search || this is Screen.Insights || this is Screen.Settings

class DiaryViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = DiaryRepository(app)
    private val pendingImages = mutableSetOf<String>()

    val settings = SettingsStore(app)

    private val driveAuth = DriveAuth(app)
    private val driveSync = DriveSync(repo)
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
    var detailEntry by mutableStateOf<DiaryEntry?>(null)
        private set

    var locked by mutableStateOf(settings.pinEnabled)
        private set
    private var stoppedAt = 0L

    // Where Detail/Editor return to: Home or the hub screen they were opened from.
    private var origin: Screen = Screen.Home

    init {
        viewModelScope.launch {
            repo.migrateLegacy()
            reloadHome()
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
        if (enabled) ReminderScheduler.schedule(context, hour, minute) else ReminderScheduler.cancel(context)
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

    private fun searchTokens() = searchQuery.lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }

    // ---- Navigation ----

    fun goBack() {
        if (screen.isHub()) origin = Screen.Home
        screen = origin
    }

    fun openHub(target: Screen) {
        origin = Screen.Home
        when (target) {
            Screen.Calendar -> {
                showMonth(calendarMonth)
                selectDay(calendarSelected)
            }
            Screen.Insights -> {
                insights = null
                viewModelScope.launch { insights = repo.insightStats(LocalDate.now()) }
            }
            Screen.Search -> runSearch()
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
            if (summary.changedLocal) afterChange()
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
        if (searchResults.isNotEmpty() || searchQuery.isNotBlank() || searchMood != null || searchTag != null) runSearch()
    }

    private companion object {
        const val LOCK_DELAY_MS = 30_000L
        const val PAGE_SIZE = 15
        const val SEARCH_PAGE = 20
    }
}
