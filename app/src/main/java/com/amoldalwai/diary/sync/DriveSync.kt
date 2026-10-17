package com.amoldalwai.diary.sync

import com.amoldalwai.diary.data.DiaryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class SyncSummary(val uploaded: Int = 0, val downloaded: Int = 0, val removed: Int = 0) {
    val changedLocal: Boolean get() = downloaded > 0 || removed > 0
}

/**
 * Two-way sync with the Drive app folder. Each entry is `e_<id>.json` and each photo `i_<file>`;
 * the newest `updatedAt` wins, and deletions travel as marker files so other devices remove the entry too.
 */
class DriveSync(private val repo: DiaryRepository) {

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
        SyncSummary(uploaded, downloaded, removed)
    }

    private companion object {
        const val ENTRY_PREFIX = "e_"
        const val ENTRY_SUFFIX = ".json"
        const val IMAGE_PREFIX = "i_"
    }
}
