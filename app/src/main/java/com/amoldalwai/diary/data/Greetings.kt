package com.amoldalwai.diary.data

import java.time.LocalDate

/** Home-screen greetings that follow the time of day; "{n}" becomes ", Name" or nothing when no name is set. */
object Greetings {
    private const val MAX_NAME = 12

    private val lateNight = listOf("Still up{n}?", "Late night thoughts{n}", "Night owl hours{n}", "The world is quiet{n}")
    private val earlyMorning = listOf("Rise and shine{n}", "Early bird{n}", "A fresh start{n}", "Good morning{n}")
    private val morning = listOf("Good morning{n}", "Hello there{n}", "Ready for today{n}?", "Make today count{n}")
    private val afternoon = listOf("Good afternoon{n}", "Hello there{n}", "Hope your day is going well{n}", "A moment for yourself{n}")
    private val evening = listOf("Good evening{n}", "Time for memories{n}", "How was your day{n}?", "Time to unwind{n}")
    private val night = listOf("Winding down{n}", "Time to reflect{n}", "Before you sleep{n}", "Quiet night{n}")

    /** First word of the profile name, shortened so it always fits on the photo; null when there is no name. */
    fun firstName(profileName: String): String? =
        profileName.trim().split(Regex("\\s+")).firstOrNull()?.take(MAX_NAME)?.takeIf { it.isNotEmpty() }

    /** The same phrase all through an hour, a different one the next hour and the next day. */
    fun greeting(hour: Int, day: LocalDate, profileName: String): String {
        val pool = when (hour) {
            in 0..4 -> lateNight
            in 5..7 -> earlyMorning
            in 8..11 -> morning
            in 12..16 -> afternoon
            in 17..20 -> evening
            else -> night
        }
        val template = pool[Math.floorMod(day.toEpochDay() * 24 + hour, pool.size.toLong()).toInt()]
        return template.replace("{n}", firstName(profileName)?.let { ", $it" }.orEmpty())
    }
}
