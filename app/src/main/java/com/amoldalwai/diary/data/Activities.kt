package com.amoldalwai.diary.data

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** One tappable thing you did today; [icon] is a key from the icon catalog or, failing that, an emoji. */
data class Activity(val name: String, val icon: String)

data class ActivityGroup(val id: String = UUID.randomUUID().toString(), val name: String, val items: List<Activity>)

object Activities {
    val defaults = listOf(
        ActivityGroup(
            "health", "Health",
            listOf(Activity("Exercise", "run"), Activity("Walk", "walk"), Activity("Sleep well", "sleep"), Activity("Water", "water"), Activity("Meditate", "meditate")),
        ),
        ActivityGroup(
            "work", "Work & study",
            listOf(Activity("Work", "work"), Activity("Study", "study"), Activity("Meeting", "meeting"), Activity("Reading", "read")),
        ),
        ActivityGroup(
            "social", "Social",
            listOf(Activity("Family", "family"), Activity("Friends", "friends"), Activity("Date", "date"), Activity("Call", "call")),
        ),
        ActivityGroup(
            "hobbies", "Hobbies",
            listOf(Activity("Gaming", "game"), Activity("Movies", "movie"), Activity("Music", "music"), Activity("Cooking", "cook")),
        ),
    )

    fun encode(groups: List<ActivityGroup>): String = JSONArray().apply {
        groups.forEach { g ->
            put(
                JSONObject().put("id", g.id).put("name", g.name).put(
                    "items",
                    JSONArray().apply { g.items.forEach { put(JSONObject().put("name", it.name).put("icon", it.icon)) } },
                ),
            )
        }
    }.toString()

    fun decode(json: String): List<ActivityGroup>? = runCatching {
        val array = JSONArray(json)
        List(array.length()) { i ->
            val g = array.getJSONObject(i)
            val items = g.getJSONArray("items")
            ActivityGroup(g.getString("id"), g.getString("name"), List(items.length()) { j ->
                val a = items.getJSONObject(j)
                Activity(a.getString("name"), a.getString("icon"))
            })
        }
    }.getOrNull()
}
