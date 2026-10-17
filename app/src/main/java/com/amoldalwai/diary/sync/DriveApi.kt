package com.amoldalwai.diary.sync

import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.UUID

class DriveAuthException : IOException("Google rejected the access token")

class DriveException(val code: Int, message: String) : IOException(message)

/** Minimal Drive v3 client for the hidden app-data folder; only files this app created are visible. */
class DriveApi(private val token: String) {

    data class RemoteFile(val id: String, val name: String, val updatedAt: Long, val deleted: Boolean)

    private class Response(val body: ByteArray)

    fun list(): List<RemoteFile> {
        val result = mutableListOf<RemoteFile>()
        var page: String? = null
        do {
            val url = StringBuilder("$API/files?spaces=appDataFolder&pageSize=1000&fields=")
                .append(encode("nextPageToken,files(id,name,appProperties)"))
            if (page != null) url.append("&pageToken=").append(encode(page))
            val json = JSONObject(String(call("GET", url.toString()).body))
            val files = json.optJSONArray("files") ?: JSONArray()
            for (i in 0 until files.length()) {
                val f = files.getJSONObject(i)
                val props = f.optJSONObject("appProperties")
                result += RemoteFile(
                    id = f.getString("id"),
                    name = f.getString("name"),
                    updatedAt = props?.optString("updatedAt")?.toLongOrNull() ?: 0L,
                    deleted = props?.optString("deleted") == "1",
                )
            }
            page = json.optString("nextPageToken").ifEmpty { null }
        } while (page != null)
        return result
    }

    /** Creates the file when [existingId] is null, otherwise replaces its content and marker properties. */
    fun upload(existingId: String?, name: String, bytes: ByteArray, mime: String, updatedAt: Long, deleted: Boolean): String {
        val metadata = JSONObject().apply {
            if (existingId == null) {
                put("name", name)
                put("parents", JSONArray().put("appDataFolder"))
            }
            put("appProperties", JSONObject().put("updatedAt", updatedAt.toString()).put("deleted", if (deleted) "1" else "0"))
        }
        val boundary = "diary" + UUID.randomUUID().toString().replace("-", "")
        val body = ByteArrayOutputStream().apply {
            write("--$boundary\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n".toByteArray())
            write(metadata.toString().toByteArray())
            write("\r\n--$boundary\r\nContent-Type: $mime\r\n\r\n".toByteArray())
            write(bytes)
            write("\r\n--$boundary--".toByteArray())
        }.toByteArray()
        val url = if (existingId == null) {
            "$UPLOAD/files?uploadType=multipart&fields=id"
        } else {
            "$UPLOAD/files/$existingId?uploadType=multipart&fields=id"
        }
        val response = call(if (existingId == null) "POST" else "PATCH", url, body, "multipart/related; boundary=$boundary")
        return JSONObject(String(response.body)).getString("id")
    }

    fun download(id: String): ByteArray = call("GET", "$API/files/$id?alt=media").body

    fun delete(id: String) {
        try {
            call("DELETE", "$API/files/$id")
        } catch (e: DriveException) {
            if (e.code != 404) throw e
        }
    }

    private fun call(method: String, url: String, body: ByteArray? = null, contentType: String? = null): Response {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            // PATCH is sent as an overridden POST so it works with every HttpURLConnection implementation.
            conn.requestMethod = if (method == "PATCH") "POST" else method
            if (method == "PATCH") conn.setRequestProperty("X-HTTP-Method-Override", "PATCH")
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.connectTimeout = 15_000
            conn.readTimeout = 60_000
            if (body != null) {
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", contentType)
                conn.setFixedLengthStreamingMode(body.size)
                conn.outputStream.use { it.write(body) }
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val bytes = stream?.use { it.readBytes() } ?: ByteArray(0)
            if (code == 401) throw DriveAuthException()
            if (code !in 200..299) throw DriveException(code, "Drive error $code: ${String(bytes).take(200)}")
            return Response(bytes)
        } finally {
            conn.disconnect()
        }
    }

    private fun encode(value: String) = URLEncoder.encode(value, "UTF-8")

    private companion object {
        const val API = "https://www.googleapis.com/drive/v3"
        const val UPLOAD = "https://www.googleapis.com/upload/drive/v3"
    }
}
