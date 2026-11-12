package com.amoldalwai.diary

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import com.amoldalwai.diary.data.ThemeMode
import com.amoldalwai.diary.reminder.HabitNudges
import com.amoldalwai.diary.reminder.ReminderScheduler
import com.amoldalwai.diary.ui.DiaryApp
import com.amoldalwai.diary.ui.theme.DiaryTheme

class MainActivity : ComponentActivity() {
    private val vm: DiaryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ReminderScheduler.ensureChannel(this)
        if (vm.settings.reminderEnabled) {
            ReminderScheduler.schedule(this, vm.settings.reminderHour, vm.settings.reminderMinute)
        }
        if (vm.settings.nudgesEnabled) HabitNudges.ensureScheduled(this, vm.settings)
        setContent {
            val dark = when (vm.settings.themeMode) {
                ThemeMode.System -> isSystemInDarkTheme()
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
            }
            DiaryTheme(darkTheme = dark, palette = vm.settings.palette) {
                DiaryApp(vm)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        vm.onAppStarted()
    }

    override fun onStop() {
        super.onStop()
        vm.onAppStopped()
    }
}
