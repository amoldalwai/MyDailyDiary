package com.amoldalwai.diary.sync

import android.accounts.Account
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.ClearTokenRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.RevokeAccessRequest
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

sealed interface AuthResult {
    data class Token(val accessToken: String) : AuthResult
    data class NeedsConsent(val pendingIntent: PendingIntent) : AuthResult
}

sealed interface SyncState {
    data object Idle : SyncState
    data object Syncing : SyncState
    data object NeedsReconnect : SyncState
    data class Error(val message: String) : SyncState
}

/** Google authorization for the Drive app-data scope; the user picks their own account. */
class DriveAuth(context: Context) {
    private val client = Identity.getAuthorizationClient(context.applicationContext)
    private val scopes = listOf(Scope(DRIVE_APPDATA), Scope(Scopes.EMAIL))

    suspend fun authorize(): AuthResult {
        val request = AuthorizationRequest.builder().setRequestedScopes(scopes).build()
        val result = client.authorize(request).await()
        val pending = result.pendingIntent
        return if (result.hasResolution() && pending != null) {
            AuthResult.NeedsConsent(pending)
        } else {
            AuthResult.Token(requireNotNull(result.accessToken) { "No access token returned" })
        }
    }

    fun tokenFromIntent(data: Intent?): String =
        requireNotNull(client.getAuthorizationResultFromIntent(data).accessToken) { "No access token returned" }

    suspend fun clearToken(token: String) {
        client.clearToken(ClearTokenRequest.builder().setToken(token).build()).await()
    }

    suspend fun revoke(email: String) {
        client.revokeAccess(
            RevokeAccessRequest.builder().setAccount(Account(email, "com.google")).setScopes(scopes).build(),
        ).await()
    }

    suspend fun userEmail(token: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            val conn = URL("https://www.googleapis.com/oauth2/v3/userinfo").openConnection() as HttpURLConnection
            try {
                conn.setRequestProperty("Authorization", "Bearer $token")
                conn.connectTimeout = 15_000
                conn.readTimeout = 15_000
                JSONObject(conn.inputStream.use { String(it.readBytes()) }).optString("email").ifEmpty { null }
            } finally {
                conn.disconnect()
            }
        }.getOrNull()
    }

    private companion object {
        const val DRIVE_APPDATA = "https://www.googleapis.com/auth/drive.appdata"
    }
}

private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { continuation.resume(it) }
    addOnFailureListener { continuation.resumeWithException(it) }
}
