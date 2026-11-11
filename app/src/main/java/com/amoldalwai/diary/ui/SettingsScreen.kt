package com.amoldalwai.diary.ui

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.text.format.Formatter
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationManagerCompat
import com.amoldalwai.diary.reminder.ReminderScheduler
import com.amoldalwai.diary.reminder.HabitNudges
import com.amoldalwai.diary.data.Habit
import com.amoldalwai.diary.data.HabitNudge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.amoldalwai.diary.data.DiaryEntry
import com.amoldalwai.diary.data.SettingsStore
import com.amoldalwai.diary.data.StorageUsage
import com.amoldalwai.diary.data.ThemeMode
import com.amoldalwai.diary.sync.SyncState
import com.amoldalwai.diary.util.PdfExporter
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class PinFlow { None, Setup, VerifyDisable, VerifyChange, ChangeNew }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: SettingsStore,
    imageFile: (String) -> File,
    loadForExport: suspend (from: Long?, to: Long?) -> List<DiaryEntry>,
    onBack: () -> Unit,
    onSetReminder: (enabled: Boolean, hour: Int, minute: Int) -> Unit,
    syncState: SyncState,
    onConnectDrive: ((PendingIntent) -> Unit) -> Unit,
    onConsentResult: (Intent?) -> Unit,
    onSyncNow: () -> Unit,
    onDisconnectDrive: () -> Unit,
    storage: StorageUsage?,
    habitCount: Int,
    habits: List<Habit>,
    onNudges: (enabled: Boolean, from: Int, to: Int, gap: Int) -> Unit,
    onHabitNudge: (habitId: String, nudge: HabitNudge) -> Unit,
    onOpenHabits: () -> Unit,
    onOpenBooks: () -> Unit,
    onOpenGames: () -> Unit,
    onOpenWatch: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    SystemBarsStyle(darkIcons = !appIsDark())

    var pinFlow by remember { mutableStateOf(PinFlow.None) }
    var showTimePicker by remember { mutableStateOf(false) }
    var confirmDisconnect by remember { mutableStateOf(false) }
    val biometricAvailable = remember { BiometricHelper.isAvailable(context) }

    val driveConsent = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        onConsentResult(result.data)
    }

    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            onSetReminder(true, settings.reminderHour, settings.reminderMinute)
        } else {
            Toast.makeText(context, "Allow notifications to get reminders", Toast.LENGTH_LONG).show()
        }
    }

    val nudgePermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            onNudges(true, settings.nudgeFrom, settings.nudgeTo, settings.nudgeGap)
        } else {
            Toast.makeText(context, "Allow notifications to get habit nudges", Toast.LENGTH_LONG).show()
        }
    }
    var nudgeTarget by remember { mutableStateOf<NudgeTarget?>(null) }

    fun toggleNudges(on: Boolean) {
        val needsPermission = on && Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        if (needsPermission) nudgePermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        else onNudges(on, settings.nudgeFrom, settings.nudgeTo, settings.nudgeGap)
    }

    fun applyPicked(target: NudgeTarget, minutes: Int) {
        val habit = target.habitId?.let { settings.nudgeFor(it) }
        val curFrom = habit?.from?.takeIf { it >= 0 } ?: settings.nudgeFrom
        val curTo = habit?.to?.takeIf { it >= 0 } ?: settings.nudgeTo
        val from = if (target.isFrom) minutes else curFrom
        val to = if (target.isFrom) curTo else minutes
        when {
            to <= from -> Toast.makeText(context, "The start has to be before the end", Toast.LENGTH_SHORT).show()
            target.habitId == null -> onNudges(settings.nudgesEnabled, from, to, settings.nudgeGap)
            else -> onHabitNudge(target.habitId, habit!!.copy(from = from, to = to))
        }
    }

    fun toggleReminder(on: Boolean) {
        val needsPermission = on && Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            onSetReminder(on, settings.reminderHour, settings.reminderMinute)
        }
    }

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(colors.background, colors.surfaceVariant)))) {
        FloatingBlobs()
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = colors.onBackground) }
                Text("Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = colors.onBackground)
            }

            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Spacer(Modifier.height(4.dp))

                SettingsCard("Personalize", Modifier.staggeredEntrance(0)) {
                    NavRow(
                        title = "Daily habits",
                        subtitle = if (habitCount == 0) "Track things like gym or 10k steps" else "$habitCount ${if (habitCount == 1) "habit" else "habits"} on your journal pages",
                        onClick = onOpenHabits,
                    )
                    Spacer(Modifier.height(12.dp))
                    ToggleRow("Habit sounds", "Play a chime when you tick a habit", settings.habitSound) { settings.updateHabitSound(it) }
                    Spacer(Modifier.height(12.dp))
                    ToggleRow("Habit nudges", "Ask about one unfinished habit at a time through the day", settings.nudgesEnabled) { toggleNudges(it) }
                    AnimatedVisibility(settings.nudgesEnabled) {
                        NudgeSettings(
                            settings = settings,
                            habits = habits,
                            onGap = { onNudges(true, settings.nudgeFrom, settings.nudgeTo, it) },
                            onPick = { nudgeTarget = it },
                            onHabitNudge = onHabitNudge,
                            onTest = {
                                scope.launch {
                                    val sent = withContext(Dispatchers.IO) { HabitNudges.nudge(context, settings, ignoreWindow = true) }
                                    if (!sent) Toast.makeText(context, "Every habit is already done today", Toast.LENGTH_SHORT).show()
                                }
                            },
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    ToggleRow("Activity chips", "Show Work, Study, Health, Social and Hobbies in Quick Add", settings.quickActivities) { settings.updateQuickActivities(it) }
                    Spacer(Modifier.height(12.dp))
                    ToggleRow("Books", "Track what you're reading on your journal pages", settings.booksEnabled) { settings.updateBooksEnabled(it) }
                    AnimatedVisibility(settings.booksEnabled) {
                        Column {
                            Spacer(Modifier.height(12.dp))
                            NavRow("Book library", "Reading, finished and want-to-read shelves", onOpenBooks)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    ToggleRow("Games", "Keep a shelf of games you play and finish", settings.gamesEnabled) { settings.updateGamesEnabled(it) }
                    AnimatedVisibility(settings.gamesEnabled) {
                        Column {
                            Spacer(Modifier.height(12.dp))
                            NavRow("Game library", "Playing, completed and backlog shelves", onOpenGames)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    ToggleRow("Movies & Series", "Keep track of what you watch", settings.watchEnabled) { settings.updateWatchEnabled(it) }
                    AnimatedVisibility(settings.watchEnabled) {
                        Column {
                            Spacer(Modifier.height(12.dp))
                            NavRow("Movies & Series library", "Watching, watched and watchlist shelves", onOpenWatch)
                        }
                    }
                }

                SettingsCard("Appearance", Modifier.staggeredEntrance(0)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeMode.entries.forEach { mode ->
                            ChoicePill(
                                label = when (mode) {
                                    ThemeMode.System -> "System"
                                    ThemeMode.Light -> "Light"
                                    ThemeMode.Dark -> "Dark"
                                },
                                selected = settings.themeMode == mode,
                                modifier = Modifier.weight(1f),
                            ) { settings.updateTheme(mode) }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("Colours", fontSize = 12.sp, color = colors.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    PalettePicker(settings.palette, darkMode = appIsDark()) { settings.updatePalette(it) }
                }

                SettingsCard("Privacy", Modifier.staggeredEntrance(1)) {
                    ToggleRow(
                        title = "App lock",
                        subtitle = "Ask for a PIN when you open the diary",
                        checked = settings.pinEnabled,
                    ) { on -> pinFlow = if (on) PinFlow.Setup else PinFlow.VerifyDisable }
                    AnimatedVisibility(settings.pinEnabled) {
                        Column {
                            if (biometricAvailable) {
                                Spacer(Modifier.height(12.dp))
                                ToggleRow(
                                    title = "Unlock with fingerprint",
                                    subtitle = "Use biometrics instead of typing your PIN",
                                    checked = settings.biometricEnabled,
                                ) { settings.updateBiometric(it) }
                            }
                            TextButton(onClick = { pinFlow = PinFlow.VerifyChange }) { Text("Change PIN") }
                        }
                    }
                }

                SettingsCard("Daily reminder", Modifier.staggeredEntrance(2)) {
                    ToggleRow(
                        title = "Remind me to write",
                        subtitle = "Skipped on days you've already written",
                        checked = settings.reminderEnabled,
                    ) { toggleReminder(it) }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Time", color = colors.onSurface, modifier = Modifier.weight(1f))
                        TextButton(onClick = { showTimePicker = true }) {
                            Text(
                                LocalTime.of(settings.reminderHour, settings.reminderMinute)
                                    .format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())),
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                    TextButton(onClick = {
                        if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                            ReminderScheduler.notifyNow(context)
                        } else {
                            Toast.makeText(context, "Notifications are turned off for Diary in system settings", Toast.LENGTH_LONG).show()
                        }
                    }) { Text("Send a test notification") }
                    if (Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true) || Build.MANUFACTURER.equals("Redmi", ignoreCase = true) || Build.MANUFACTURER.equals("POCO", ignoreCase = true)) {
                        Text(
                            "Xiaomi tip: if reminders don't arrive, open Settings → Apps → Diary and set Battery saver to \"No restrictions\" and turn on Autostart.",
                            fontSize = 12.sp,
                            color = colors.onSurfaceVariant,
                        )
                    }
                }

                SettingsCard("Google Drive sync", Modifier.staggeredEntrance(3)) {
                    if (!settings.driveConnected) {
                        Text(
                            "Keep your diary safe in your own Google Drive and in sync across your phones. " +
                                "Entries and photos go to a private folder that only this app can open.",
                            color = colors.onSurfaceVariant,
                            fontSize = 14.sp,
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = {
                            onConnectDrive { pending ->
                                driveConsent.launch(IntentSenderRequest.Builder(pending.intentSender).build())
                            }
                        }) { Text("Connect Google account") }
                    } else {
                        Text("Signed in as", fontSize = 12.sp, color = colors.onSurfaceVariant)
                        Text(settings.driveEmail.orEmpty(), color = colors.onSurface, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (syncState is SyncState.Syncing) {
                                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(
                                when (syncState) {
                                    SyncState.Syncing -> "Syncing…"
                                    SyncState.NeedsReconnect -> "Google Drive access expired. Disconnect and connect again."
                                    is SyncState.Error -> syncState.message
                                    SyncState.Idle ->
                                        if (settings.lastSyncAt == 0L) {
                                            "Not synced yet"
                                        } else {
                                            "Last synced " + Instant.ofEpochMilli(settings.lastSyncAt).atZone(ZoneId.systemDefault())
                                                .format(DateTimeFormatter.ofPattern("d MMM, h:mm a", Locale.getDefault()))
                                        }
                                },
                                fontSize = 13.sp,
                                color = if (syncState is SyncState.Error || syncState is SyncState.NeedsReconnect) colors.error else colors.onSurfaceVariant,
                            )
                        }
                        Row {
                            TextButton(onClick = onSyncNow, enabled = syncState !is SyncState.Syncing) { Text("Sync now") }
                            TextButton(onClick = { confirmDisconnect = true }) { Text("Disconnect") }
                        }
                    }
                }

                SettingsCard("Storage", Modifier.staggeredEntrance(4)) {
                    if (storage == null) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        fun size(bytes: Long) = Formatter.formatShortFileSize(context, bytes)
                        Text(
                            size(storage.totalBytes),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurface,
                        )
                        Text("used by Diary on this phone", fontSize = 13.sp, color = colors.onSurfaceVariant)
                        Spacer(Modifier.height(14.dp))
                        val photoShare = if (storage.totalBytes == 0L) 0f else storage.photoBytes.toFloat() / storage.totalBytes
                        Row(Modifier.fillMaxWidth().height(10.dp).clip(CircleShape).background(colors.surfaceVariant)) {
                            if (photoShare > 0f) {
                                Box(Modifier.weight(photoShare.coerceAtLeast(0.02f)).fillMaxHeight().background(colors.primary))
                            }
                            if (photoShare < 1f) {
                                Box(Modifier.weight((1f - photoShare).coerceAtLeast(0.02f)).fillMaxHeight().background(colors.tertiary))
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        StorageLine(colors.primary, "Photos", "${storage.photoCount} ${if (storage.photoCount == 1) "photo" else "photos"}", size(storage.photoBytes))
                        Spacer(Modifier.height(8.dp))
                        StorageLine(colors.tertiary, "Entries and text", "${storage.entryCount} ${if (storage.entryCount == 1) "entry" else "entries"}", size(storage.dataBytes))
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Photos are saved at a reduced size to keep this small. Turning on Google Drive sync stores a copy in your own Drive, which counts toward your Google storage.",
                            fontSize = 12.sp,
                            color = colors.onSurfaceVariant,
                        )
                    }
                }

                SettingsCard("Your data", Modifier.staggeredEntrance(5)) {
                    Text(
                        "Everything is stored privately on this device.",
                        color = colors.onSurfaceVariant,
                        fontSize = 14.sp,
                    )
                    Spacer(Modifier.height(10.dp))
                    TextButton(onClick = {
                        scope.launch {
                            val entries = loadForExport(null, null)
                            if (entries.isEmpty()) {
                                Toast.makeText(context, "No entries to export yet", Toast.LENGTH_SHORT).show()
                            } else {
                                PdfExporter.exportAndShare(context, entries, "My_Diary", "My Diary", imageFile)
                            }
                        }
                    }) { Text("Export all entries as PDF") }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (confirmDisconnect) {
        AlertDialog(
            onDismissRequest = { confirmDisconnect = false },
            title = { Text("Disconnect Google Drive?") },
            text = { Text("Syncing stops. Your entries stay on this phone and the copy in your Drive is kept.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDisconnect = false
                    onDisconnectDrive()
                }) { Text("Disconnect") }
            },
            dismissButton = { TextButton(onClick = { confirmDisconnect = false }) { Text("Cancel") } },
        )
    }

    nudgeTarget?.let { target ->
        val n = target.habitId?.let { settings.nudgeFor(it) }
        val initial = if (target.isFrom) n?.from?.takeIf { it >= 0 } ?: settings.nudgeFrom else n?.to?.takeIf { it >= 0 } ?: settings.nudgeTo
        val state = rememberTimePickerState(initial / 60, initial % 60, is24Hour = false)
        AlertDialog(
            onDismissRequest = { nudgeTarget = null },
            title = { Text(if (target.isFrom) "Start asking from" else "Stop asking after") },
            confirmButton = {
                TextButton(onClick = {
                    applyPicked(target, state.hour * 60 + state.minute)
                    nudgeTarget = null
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { nudgeTarget = null }) { Text("Cancel") } },
            text = { TimePicker(state) },
        )
    }

    if (showTimePicker) {
        val state = rememberTimePickerState(settings.reminderHour, settings.reminderMinute, is24Hour = false)
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    onSetReminder(settings.reminderEnabled, state.hour, state.minute)
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } },
            text = { TimePicker(state) },
        )
    }

    when (pinFlow) {
        PinFlow.None -> Unit
        PinFlow.Setup -> PinDialog(
            title = "Set a PIN",
            confirm = true,
            onDismiss = { pinFlow = PinFlow.None },
            onSuccess = {
                settings.setPin(it)
                pinFlow = PinFlow.None
            },
        )
        PinFlow.VerifyDisable -> PinDialog(
            title = "Enter your PIN",
            confirm = false,
            onDismiss = { pinFlow = PinFlow.None },
            verify = settings::verifyPin,
            onSuccess = {
                settings.clearPin()
                pinFlow = PinFlow.None
            },
        )
        PinFlow.VerifyChange -> PinDialog(
            title = "Enter current PIN",
            confirm = false,
            onDismiss = { pinFlow = PinFlow.None },
            verify = settings::verifyPin,
            onSuccess = { pinFlow = PinFlow.ChangeNew },
        )
        PinFlow.ChangeNew -> PinDialog(
            title = "Choose a new PIN",
            confirm = true,
            onDismiss = { pinFlow = PinFlow.None },
            onSuccess = {
                settings.setPin(it)
                pinFlow = PinFlow.None
            },
        )
    }
}

@Composable
private fun SettingsCard(title: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(colors.surface.copy(alpha = 0.8f))
            .border(1.dp, colors.outline.copy(alpha = 0.4f), RoundedCornerShape(26.dp))
            .padding(18.dp),
    ) {
        Text(
            title.uppercase(),
            fontSize = 11.sp,
            letterSpacing = 1.8.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.primary,
        )
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun NavRow(title: String, subtitle: String, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium, color = colors.onSurface)
            Text(subtitle, fontSize = 13.sp, color = colors.onSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = colors.onSurfaceVariant)
    }
}

@Composable
private fun StorageLine(color: androidx.compose.ui.graphics.Color, title: String, detail: String, size: String) {
    val colors = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = colors.onSurface, fontWeight = FontWeight.Medium)
            Text(detail, fontSize = 12.sp, color = colors.onSurfaceVariant)
        }
        Text(size, color = colors.onSurface, fontWeight = FontWeight.SemiBold)
    }
}

/** Which end of which window is being edited; a null [habitId] is the shared window. */
private data class NudgeTarget(val habitId: String?, val isFrom: Boolean)

private val nudgeGaps = listOf(30 to "30 min", 60 to "1 h", 90 to "1.5 h", 120 to "2 h", 180 to "3 h")

private fun clock(minutes: Int): String =
    LocalTime.of(minutes / 60, minutes % 60).format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))

@Composable
private fun NudgeSettings(
    settings: SettingsStore,
    habits: List<Habit>,
    onGap: (Int) -> Unit,
    onPick: (NudgeTarget) -> Unit,
    onHabitNudge: (String, HabitNudge) -> Unit,
    onTest: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Column {
        Spacer(Modifier.height(14.dp))
        Text("Ask between", fontWeight = FontWeight.Medium, color = colors.onSurface)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TimeChip(clock(settings.nudgeFrom), Modifier.weight(1f)) { onPick(NudgeTarget(null, true)) }
            Text("to", color = colors.onSurfaceVariant, modifier = Modifier.align(Alignment.CenterVertically))
            TimeChip(clock(settings.nudgeTo), Modifier.weight(1f)) { onPick(NudgeTarget(null, false)) }
        }
        Spacer(Modifier.height(14.dp))
        Text("Time between nudges", fontWeight = FontWeight.Medium, color = colors.onSurface)
        Text("Only one habit is asked about at a time, and a habit you've done isn't asked again today.", fontSize = 12.sp, color = colors.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            nudgeGaps.forEach { (minutes, label) -> ChoicePill(label, settings.nudgeGap == minutes, Modifier.weight(1f)) { onGap(minutes) } }
        }
        Spacer(Modifier.height(14.dp))
        Text("Habits to ask about", fontWeight = FontWeight.Medium, color = colors.onSurface)
        Spacer(Modifier.height(6.dp))
        if (habits.isEmpty()) {
            Text("Add a habit first and it will show up here.", fontSize = 13.sp, color = colors.onSurfaceVariant)
        }
        habits.forEach { habit ->
            val nudge = settings.nudgeFor(habit.id)
            val custom = nudge.from >= 0 && nudge.to >= 0
            Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${habit.emoji}  ${habit.name}", color = colors.onSurface, modifier = Modifier.weight(1f))
                    Switch(checked = nudge.on, onCheckedChange = { onHabitNudge(habit.id, nudge.copy(on = it)) })
                }
                if (nudge.on) {
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        TimeChip(clock(if (custom) nudge.from else settings.nudgeFrom), Modifier.weight(1f), faded = !custom) { onPick(NudgeTarget(habit.id, true)) }
                        Text("to", fontSize = 12.sp, color = colors.onSurfaceVariant)
                        TimeChip(clock(if (custom) nudge.to else settings.nudgeTo), Modifier.weight(1f), faded = !custom) { onPick(NudgeTarget(habit.id, false)) }
                        if (custom) {
                            TextButton(onClick = { onHabitNudge(habit.id, nudge.copy(from = -1, to = -1)) }) { Text("Reset", fontSize = 12.sp) }
                        }
                    }
                    if (!custom) Text("Uses the shared window", fontSize = 11.sp, color = colors.onSurfaceVariant)
                }
            }
        }
        TextButton(onClick = onTest) { Text("Send a test nudge") }
    }
}

@Composable
private fun TimeChip(label: String, modifier: Modifier = Modifier, faded: Boolean = false, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier.bouncyClick(onClick).clip(RoundedCornerShape(14.dp)).background(colors.surfaceVariant).padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = if (faded) colors.onSurfaceVariant else colors.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium, color = colors.onSurface)
            Text(subtitle, fontSize = 13.sp, color = colors.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}

@Composable
private fun ChoicePill(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val fill by animateColorAsState(if (selected) colors.primary else colors.surfaceVariant, label = "choiceFill")
    val text by animateColorAsState(if (selected) colors.onPrimary else colors.onSurface, label = "choiceText")
    Box(
        modifier
            .bouncyClick(onClick)
            .clip(RoundedCornerShape(16.dp))
            .background(fill)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = text, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium)
    }
}
