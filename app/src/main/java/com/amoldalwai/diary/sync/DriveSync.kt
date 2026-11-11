package com.amoldalwai.diary.sync

import com.amoldalwai.diary.data.Book
import com.amoldalwai.diary.data.BooksRepository
import com.amoldalwai.diary.data.DiaryRepository
import com.amoldalwai.diary.data.GamesRepository
import com.amoldalwai.diary.data.HabitsRepository
import com.amoldalwai.diary.data.WatchRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class SyncSummary(val uploaded: Int = 0, val downloaded: Int = 0, val removed: Int = 0) {
    val changedLocal: Boolean get() = downloaded > 0 || removed > 0
}

/**
 * Two-way sync with the Drive app folder. Each entry is `e_<id>.json` and each photo `i_<file>`;
 * the newest `updatedAt` wins, and deletions travel as marker files so other devices remove the entry too.
 */
class DriveSync(
    private val repo: DiaryRepository,
    private val habitsRepo: HabitsRepository,
    private val booksRepo: BooksRepository,
    private val gamesRepo: GamesRepository,
    private val watchRepo: WatchRepository,
) {

    suspend fun run(token: String): SyncSummary = withContext(Dispatchers.IO) {
        val api = DriveApi(token)
        val remote = api.list()
        val remoteEntries = remote.filter { it.name.startsWith(ENTRY_PREFIX) }
            .associateBy { it.name.removePrefix(ENTRY_PREFIX).removeSuffix(ENTRY_SUFFIX) }
        val remoteImages = remote.filter { it.name.startsWith(IMAGE_PREFIX) }
            .associateBy { it.name.removePrefix(IMAGE_PREFIX) }
            .toMutableMap()

        val local = repo.syncIndex()
        val tombstones = repo.tombstones()
        var uploaded = 0
        var downloaded = 0
        var removed = 0

        suspend fun push(id: String, existing: DriveApi.RemoteFile?) {
            val entry = repo.getEntry(id) ?: return
            for (name in entry.images) {
                val file = repo.imageFile(name)
                if (name !in remoteImages && file.exists()) {
                    val fileId = api.upload(null, IMAGE_PREFIX + name, file.readBytes(), "image/jpeg", entry.updatedAt, false)
                    remoteImages[name] = DriveApi.RemoteFile(fileId, IMAGE_PREFIX + name, entry.updatedAt, false)
                }
            }
            api.upload(existing?.id, "$ENTRY_PREFIX$id$ENTRY_SUFFIX", SyncCodec.encode(entry).toByteArray(), "application/json", entry.updatedAt, false)
            uploaded++
        }

        suspend fun pull(file: DriveApi.RemoteFile) {
            val entry = SyncCodec.decode(String(api.download(file.id)))
            for (name in entry.images) {
                val target = repo.imageFile(name)
                val remoteImage = remoteImages[name]
                if (!target.exists() && remoteImage != null) target.writeBytes(api.download(remoteImage.id))
            }
            repo.applyRemote(entry)
            downloaded++
        }

        val ids = local.keys + remoteEntries.keys + tombstones.keys
        for (id in ids) {
            val localUpdated = local[id]
            val remoteFile = remoteEntries[id]
            val tombstone = tombstones[id]
            when {
                localUpdated != null && (remoteFile == null || (remoteFile.deleted && localUpdated > remoteFile.updatedAt)) ->
                    push(id, remoteFile)

                localUpdated != null && remoteFile != null && !remoteFile.deleted -> when {
                    localUpdated > remoteFile.updatedAt -> push(id, remoteFile)
                    localUpdated < remoteFile.updatedAt -> pull(remoteFile)
                }

                localUpdated != null && remoteFile != null -> {
                    repo.applyRemoteDelete(id)
                    removed++
                }

                remoteFile != null && !remoteFile.deleted -> {
                    if (tombstone != null && tombstone.deletedAt >= remoteFile.updatedAt) {
                        api.upload(remoteFile.id, remoteFile.name, "{}".toByteArray(), "application/json", tombstone.deletedAt, true)
                        tombstone.images.forEach { name -> remoteImages.remove(name)?.let { api.delete(it.id) } }
                        repo.clearTombstone(id)
                    } else {
                        pull(remoteFile)
                    }
                }

                else -> if (tombstone != null) repo.clearTombstone(id)
            }
        }
        // Habits: one file each, holding every tick; two copies are merged tick by tick (newest wins).
        val remoteHabits = remote.filter { it.name.startsWith(HABIT_PREFIX) }
            .associateBy { it.name.removePrefix(HABIT_PREFIX).removeSuffix(ENTRY_SUFFIX) }
        val localHabits = habitsRepo.allRecords().associateBy { it.habit.id }
        for (id in localHabits.keys + remoteHabits.keys) {
            val mine = localHabits[id]
            val theirs = remoteHabits[id]
            val name = "$HABIT_PREFIX$id$ENTRY_SUFFIX"
            when {
                mine != null && theirs == null -> {
                    api.upload(null, name, PersonalCodec.encode(mine).toByteArray(), JSON, mine.maxUpdated, false)
                    uploaded++
                }

                mine == null && theirs != null -> {
                    habitsRepo.applyRecord(PersonalCodec.decodeHabit(String(api.download(theirs.id))))
                    downloaded++
                }

                mine != null && theirs != null && mine.maxUpdated != theirs.updatedAt -> {
                    val merged = PersonalCodec.merge(mine, PersonalCodec.decodeHabit(String(api.download(theirs.id))))
                    habitsRepo.applyRecord(merged)
                    if (theirs.updatedAt > mine.maxUpdated) downloaded++
                    if (merged.maxUpdated > theirs.updatedAt) {
                        api.upload(theirs.id, name, PersonalCodec.encode(merged).toByteArray(), JSON, merged.maxUpdated, false)
                        uploaded++
                    }
                }
            }
        }

        // Books: same idea, plus the cover image stored with the photos.
        val remoteBooks = remote.filter { it.name.startsWith(BOOK_PREFIX) }
            .associateBy { it.name.removePrefix(BOOK_PREFIX).removeSuffix(ENTRY_SUFFIX) }
        val localBooks = booksRepo.allRecords().associateBy { it.book.id }

        fun uploadCover(cover: String?) {
            if (cover == null || cover in remoteImages) return
            val file = booksRepo.imageFile(cover)
            if (!file.exists()) return
            val fileId = api.upload(null, IMAGE_PREFIX + cover, file.readBytes(), "image/jpeg", System.currentTimeMillis(), false)
            remoteImages[cover] = DriveApi.RemoteFile(fileId, IMAGE_PREFIX + cover, System.currentTimeMillis(), false)
        }

        fun fetchCover(cover: String?, deleted: Boolean) {
            cover ?: return
            val file = booksRepo.imageFile(cover)
            if (deleted) {
                file.delete()
                remoteImages.remove(cover)?.let { api.delete(it.id) }
            } else if (!file.exists()) {
                remoteImages[cover]?.let { file.writeBytes(api.download(it.id)) }
            }
        }

        for (id in localBooks.keys + remoteBooks.keys) {
            val mine = localBooks[id]
            val theirs = remoteBooks[id]
            val name = "$BOOK_PREFIX$id$ENTRY_SUFFIX"
            when {
                mine != null && theirs == null -> {
                    if (!mine.book.deleted) uploadCover(mine.book.cover)
                    api.upload(null, name, PersonalCodec.encode(mine).toByteArray(), JSON, mine.maxUpdated, false)
                    uploaded++
                }

                mine == null && theirs != null -> {
                    val record = PersonalCodec.decodeBook(String(api.download(theirs.id)))
                    fetchCover(record.book.cover, record.book.deleted)
                    booksRepo.applyRecord(record)
                    downloaded++
                }

                mine != null && theirs != null && mine.maxUpdated != theirs.updatedAt -> {
                    val merged = PersonalCodec.merge(mine, PersonalCodec.decodeBook(String(api.download(theirs.id))))
                    fetchCover(merged.book.cover, merged.book.deleted)
                    booksRepo.applyRecord(merged)
                    if (theirs.updatedAt > mine.maxUpdated) downloaded++
                    if (merged.maxUpdated > theirs.updatedAt) {
                        if (!merged.book.deleted) uploadCover(merged.book.cover)
                        api.upload(theirs.id, name, PersonalCodec.encode(merged).toByteArray(), JSON, merged.maxUpdated, false)
                        uploaded++
                    }
                }
            }
        }

        // Games: one small file each, newest edit wins; covers travel with the photos.
        val remoteGames = remote.filter { it.name.startsWith(GAME_PREFIX) }
            .associateBy { it.name.removePrefix(GAME_PREFIX).removeSuffix(ENTRY_SUFFIX) }
        val localGames = gamesRepo.allGames().associateBy { it.id }
        for (id in localGames.keys + remoteGames.keys) {
            val mine = localGames[id]
            val theirs = remoteGames[id]
            val name = "$GAME_PREFIX$id$ENTRY_SUFFIX"
            when {
                mine != null && theirs == null -> {
                    if (!mine.deleted) uploadCover(mine.cover)
                    api.upload(null, name, PersonalCodec.encode(mine).toByteArray(), JSON, mine.updatedAt, false)
                    uploaded++
                }

                mine == null && theirs != null -> {
                    val game = PersonalCodec.decodeGame(String(api.download(theirs.id)))
                    fetchCover(game.cover, game.deleted)
                    gamesRepo.apply(game)
                    downloaded++
                }

                mine != null && theirs != null && mine.updatedAt > theirs.updatedAt -> {
                    if (!mine.deleted) uploadCover(mine.cover)
                    api.upload(theirs.id, name, PersonalCodec.encode(mine).toByteArray(), JSON, mine.updatedAt, false)
                    uploaded++
                }

                mine != null && theirs != null && mine.updatedAt < theirs.updatedAt -> {
                    val game = PersonalCodec.decodeGame(String(api.download(theirs.id)))
                    fetchCover(game.cover, game.deleted)
                    gamesRepo.apply(game)
                    downloaded++
                }
            }
        }
        // Movies and series: one small file each, newest edit wins; posters travel with the photos.
        val remoteWatch = remote.filter { it.name.startsWith(WATCH_PREFIX) }
            .associateBy { it.name.removePrefix(WATCH_PREFIX).removeSuffix(ENTRY_SUFFIX) }
        val localWatch = watchRepo.allItems().associateBy { it.id }
        for (id in localWatch.keys + remoteWatch.keys) {
            val mine = localWatch[id]
            val theirs = remoteWatch[id]
            val name = "$WATCH_PREFIX$id$ENTRY_SUFFIX"
            when {
                mine != null && theirs == null -> {
                    if (!mine.deleted) uploadCover(mine.cover)
                    api.upload(null, name, PersonalCodec.encode(mine).toByteArray(), JSON, mine.updatedAt, false)
                    uploaded++
                }

                mine == null && theirs != null -> {
                    val item = PersonalCodec.decodeWatch(String(api.download(theirs.id)))
                    fetchCover(item.cover, item.deleted)
                    watchRepo.apply(item)
                    downloaded++
                }

                mine != null && theirs != null && mine.updatedAt > theirs.updatedAt -> {
                    if (!mine.deleted) uploadCover(mine.cover)
                    api.upload(theirs.id, name, PersonalCodec.encode(mine).toByteArray(), JSON, mine.updatedAt, false)
                    uploaded++
                }

                mine != null && theirs != null && mine.updatedAt < theirs.updatedAt -> {
                    val item = PersonalCodec.decodeWatch(String(api.download(theirs.id)))
                    fetchCover(item.cover, item.deleted)
                    watchRepo.apply(item)
                    downloaded++
                }
            }
        }
        SyncSummary(uploaded, downloaded, removed)
    }

    private companion object {
        const val ENTRY_PREFIX = "e_"
        const val ENTRY_SUFFIX = ".json"
        const val IMAGE_PREFIX = "i_"
        const val HABIT_PREFIX = "h_"
        const val BOOK_PREFIX = "b_"
        const val GAME_PREFIX = "g_"
        const val WATCH_PREFIX = "w_"
        const val JSON = "application/json"
    }
}
