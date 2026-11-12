package com.amoldalwai.diary

import android.content.Context
import android.content.pm.ActivityInfo
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amoldalwai.diary.data.Book
import com.amoldalwai.diary.data.BookStatus
import com.amoldalwai.diary.data.BooksRepository
import com.amoldalwai.diary.data.DiaryEntry
import com.amoldalwai.diary.data.DiaryRepository
import com.amoldalwai.diary.data.FinishInfo
import com.amoldalwai.diary.data.Game
import com.amoldalwai.diary.data.GameStatus
import com.amoldalwai.diary.data.GamesRepository
import com.amoldalwai.diary.data.Habit
import com.amoldalwai.diary.data.HabitsRepository
import com.amoldalwai.diary.data.Mood
import com.amoldalwai.diary.data.SettingsStore
import com.amoldalwai.diary.data.ThemeMode
import com.amoldalwai.diary.data.WatchItem
import com.amoldalwai.diary.data.WatchKind
import com.amoldalwai.diary.data.WatchRepository
import com.amoldalwai.diary.data.WatchResult
import com.amoldalwai.diary.data.WatchStatus
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * Fills a fresh install with mock data (including awkward cases), then visits every screen in light and dark,
 * portrait and landscape. Any crash fails the test.
 */
@RunWith(AndroidJUnit4::class)
class MockDataSmokeTest {
    @get:Rule val compose = createEmptyComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private var scenario: ActivityScenario<MainActivity>? = null
    private lateinit var firstEntryId: String

    @Before
    fun seed() {
        runBlocking { seedAll() }
    }

    private suspend fun seedAll() {
        val today = LocalDate.now().toEpochDay()
        val diary = DiaryRepository(context)
        val moods = Mood.entries
        val awkward = listOf(
            "" to "",
            "A".repeat(220) to "word ".repeat(1500),
            "Emoji 😀🥰🤩 and ünïcödé" to "Line one\n\n\n\nLine five 🌍\n" + "x".repeat(6000),
            "مرحبا بالعالم" to "نص عربي طويل ".repeat(200),
            "日本語のタイトル" to "これは日記です。".repeat(300),
            "<b>html & \"quotes\" 'single'</b>" to "SELECT * FROM entries; -- % _ \\ ' \"",
        )
        for (i in 0 until 160) {
            if (i % 7 == 3) continue
            val (title, body) = awkward.getOrNull(i) ?: ("Day $i" to "Mock note number $i. ".repeat(1 + i % 12))
            val entry = DiaryEntry(
                dateEpochDay = today - i - (if (i > 120) 100 else 0),
                title = title,
                body = body,
                mood = if (i % 5 == 4) null else moods[i % moods.size],
                tags = listOf("work", "family", "travel", "tag$i", "a-really-long-tag-name-to-test-wrapping").take(1 + i % 5),
            )
            if (i == 1) firstEntryId = entry.id
            diary.save(entry)
        }
        // A future-dated and a far-past entry.
        diary.save(DiaryEntry(dateEpochDay = today + 400, title = "Future", body = "from the future"))
        diary.save(DiaryEntry(dateEpochDay = LocalDate.of(2001, 1, 1).toEpochDay(), title = "Old", body = "ancient"))

        val habits = HabitsRepository(context)
        val names = listOf("Walk" to "🚶", "Read 20 pages" to "📖", "A habit with a very very long name that keeps going" to "💧", "Meditate" to "🧘", "Code" to "💻")
        names.forEachIndexed { index, (name, emoji) ->
            val habit = Habit(name = name, emoji = emoji, position = index)
            habits.save(habit)
            for (d in 0 until 90) if ((d + index) % (index + 2) != 0) habits.setDone(habit.id, today - d, true)
        }

        val books = BooksRepository(context)
        BookStatus.entries.forEachIndexed { s, status ->
            for (n in 0 until 4) {
                books.save(
                    Book(
                        title = if (n == 3) "An Extremely Long Book Title That Should Wrap Across Many Lines And Never Break The Layout $s" else "Book $s-$n",
                        authors = if (n == 2) "" else "Author $n, Second Author",
                        status = status,
                        totalPages = if (n == 1) null else 300 + n,
                        currentPage = if (status == BookStatus.Reading) 120 else 0,
                        rating = if (status == BookStatus.Finished) 1 + n * 3 else null,
                        review = if (n == 0) "Loved it. ".repeat(80) else "",
                        finishedAt = if (status == BookStatus.Finished) System.currentTimeMillis() - n * 86_400_000L else null,
                    ),
                )
            }
        }

        val games = GamesRepository(context)
        GameStatus.entries.forEachIndexed { s, status ->
            for (n in 0 until 3) {
                games.save(Game(title = "Game $s-$n ${"long ".repeat(n * 4)}", platform = listOf("PC", "PlayStation", "Switch")[n], status = status, rating = if (n == 0) 9 else null))
            }
        }

        val watch = WatchRepository(context)
        WatchKind.entries.forEach { kind ->
            WatchStatus.entries.forEachIndexed { s, status ->
                for (n in 0 until 3) {
                    watch.save(
                        WatchItem(
                            kind = kind,
                            title = "${kind.name} $s-$n ${"title ".repeat(n * 3)}",
                            year = 1990 + n * 10,
                            status = status,
                            season = if (kind == WatchKind.Series) 1 + n else 0,
                            episode = if (kind == WatchKind.Series) n * 4 else 0,
                            rating = if (n == 1) 8 else null,
                        ),
                    )
                }
            }
        }

        SettingsStore(context).apply {
            markOnboardingDone()
            updateProfile("Amol Dalwai Extraordinarylongsurname", "🙂", "Mock profile", -1)
        }
    }

    @After
    fun close() {
        scenario?.close()
    }

    private lateinit var vm: DiaryViewModel

    private fun settle(ms: Long = 900) {
        compose.waitForIdle()
        Thread.sleep(ms)
        compose.waitForIdle()
    }

    private fun act(block: DiaryViewModel.() -> Unit) {
        scenario!!.onActivity { vm.block() }
        settle()
    }

    private fun scrollDown() {
        repeat(3) {
            runCatching { compose.onAllNodes(isRoot()).onFirst().performTouchInput { swipeUp() } }
            settle(300)
        }
    }

    private fun visitEverything() {
        val tabs = listOf(Screen.Home, Screen.Journal, Screen.Library, Screen.More)
        tabs.forEach { tab ->
            act { openTab(tab) }
            scrollDown()
        }
        listOf(
            Screen.Calendar, Screen.Search, Screen.Insights, Screen.Settings, Screen.Album, Screen.Habits,
            Screen.Books, Screen.Games, Screen.Watch, Screen.Profile, Screen.LevelPath, Screen.About,
        ).forEach { hub ->
            act { openTab(Screen.More) }
            act { openHub(hub) }
            scrollDown()
            act { goBack() }
        }
        act { openTab(Screen.Home); openQuickAdd(Mood.Happy) }
        act { closeQuickAdd() }
        act { openTab(Screen.Journal); openDetail(firstEntryId) }
        scrollDown()
        act { goBack() }
        act { openNew() }
        act { goBack() }
        act { searchQuery = "Mock"; runSearch() }
        act { openHub(Screen.Search); runSearch() }
        act { goBack() }
    }

    @Test
    fun finishingAndPastPageRules() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario!!.onActivity { vm = ViewModelProvider(it)[DiaryViewModel::class.java] }
        settle(2500)

        // Finishing a book asks for a rating (3 preselected) and saves nothing until it is given.
        val book = vm.books.first { it.status == BookStatus.Reading && it.totalPages != null }
        act { updateBook(book.copy(status = BookStatus.Finished)) }
        assertEquals(3, vm.finishRequest?.startRating)
        assertEquals(BookStatus.Reading, vm.books.first { it.id == book.id }.status)
        act { answerFinish(4) }
        val finished = vm.books.first { it.id == book.id }
        assertEquals(BookStatus.Finished, finished.status)
        assertEquals(4, finished.rating)
        assertTrue(finished.finishedAt != null)

        // Reaching the last page finishes a book the same way.
        val reading = vm.books.first { it.status == BookStatus.Reading && it.totalPages != null }
        act { setBookPage(reading, LocalDate.now(), reading.totalPages!!) }
        assertTrue("Last page should ask for a rating", vm.finishRequest != null)
        act { answerFinish(null) }
        assertEquals(BookStatus.Reading, vm.books.first { it.id == reading.id }.status)

        // A movie needs a rating too, and backing out leaves it on the watchlist.
        val movie = vm.watchItems.first { it.status == WatchStatus.Watchlist }
        act { updateWatch(movie.copy(status = WatchStatus.Watched)) }
        assertEquals(null, vm.finishRequest?.startRating)
        act { answerFinish(null) }
        assertEquals(WatchStatus.Watchlist, vm.watchItems.first { it.id == movie.id }.status)

        // Added as watched "a long time ago": rated, but with no date at all.
        act { addWatch(WatchResult(WatchKind.Movie, "Old Movie", 1999, "", null, null, "Manual"), WatchKind.Movie, WatchStatus.Watched, 0, 0, FinishInfo(7, null)) {} }
        val old = vm.watchItems.first { it.title == "Old Movie" }
        assertEquals(WatchStatus.Watched, old.status)
        assertEquals(7, old.rating)
        assertEquals(null, old.finishedAt)
        assertEquals(null, old.finishedYear)

        // Added as watched in a given year: only the year is kept.
        act { addWatch(WatchResult(WatchKind.Movie, "Year Movie", 2010, "", null, null, "Manual"), WatchKind.Movie, WatchStatus.Watched, 0, 0, FinishInfo(8, 2023)) {} }
        val dated = vm.watchItems.first { it.title == "Year Movie" }
        assertEquals(2023, dated.finishedYear)
        assertEquals(null, dated.finishedAt)

        // Past pages can be read but not edited.
        act { openDetail(firstEntryId) }
        assertTrue(vm.detailEntry!!.date.isBefore(LocalDate.now()))
        act { openEditor(vm.detailEntry!!) }
        assertTrue("Past page must stay read-only", vm.screen is Screen.Detail)

        // A quick note needs text.
        val before = vm.homeStats.total
        act { saveQuickEntry(Mood.Happy, emptyList(), "  ", emptyList()) }
        assertEquals(before, vm.homeStats.total)
    }

    @Test
    fun everyScreenSurvivesMockData() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario!!.onActivity { vm = ViewModelProvider(it)[DiaryViewModel::class.java] }
        settle(2500)
        assertTrue("Seeded entries should load", vm.homeEntries.isNotEmpty())
        assertEquals("Seeded books", 12, vm.books.size)
        assertEquals("Seeded habits", 5, vm.habits.size)

        // Light, portrait.
        scenario!!.onActivity { vm.settings.updateTheme(ThemeMode.Light) }
        visitEverything()

        // Dark, portrait.
        scenario!!.onActivity { vm.settings.updateTheme(ThemeMode.Dark) }
        settle()
        visitEverything()

        // Dark, landscape.
        scenario!!.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
        settle(1500)
        scenario!!.onActivity { vm = ViewModelProvider(it)[DiaryViewModel::class.java] }
        visitEverything()

        // Light, landscape.
        scenario!!.onActivity { vm.settings.updateTheme(ThemeMode.Light) }
        settle()
        visitEverything()
    }
}
