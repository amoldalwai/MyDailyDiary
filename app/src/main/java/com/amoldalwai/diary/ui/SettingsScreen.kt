package com.amoldalwai.diary.ui

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import com.amoldalwai.diary.data.DiaryEntry
import com.amoldalwai.diary.data.SettingsStore
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

                SettingsCard("Your data", Modifier.staggeredEntrance(4)) {
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
