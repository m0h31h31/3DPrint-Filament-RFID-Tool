package com.m0h31h31.bamburfidreader.cloud

import android.content.Context

class SharedPreferencesBambuCloudSessionStore(
    context: Context
) : BambuCloudSessionStore {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun loadSession(): BambuCloudSession? {
        val accessToken = prefs.getString(KEY_ACCESS_TOKEN, "").orEmpty()
        if (accessToken.isBlank()) return null
        val account = BambuCloudAccount(
            uid = prefs.getLong(KEY_UID, 0L),
            name = prefs.getString(KEY_NAME, "").orEmpty(),
            handle = prefs.getString(KEY_HANDLE, "").orEmpty(),
            avatarUrl = prefs.getString(KEY_AVATAR_URL, "").orEmpty(),
            bio = prefs.getString(KEY_BIO, "").orEmpty()
        )
        val tokens = BambuCloudTokens(
            accessToken = accessToken,
            refreshToken = prefs.getString(KEY_REFRESH_TOKEN, "").orEmpty(),
            expiresInSeconds = prefs.getInt(KEY_EXPIRES_IN_SECONDS, 0),
            loginType = prefs.getString(KEY_LOGIN_TYPE, "").orEmpty()
        )
        return BambuCloudSession(
            tokens = tokens,
            account = account,
            savedAtMillis = prefs.getLong(KEY_SAVED_AT_MS, 0L),
            expiresAtMillis = prefs.getLong(KEY_EXPIRES_AT_MS, 0L),
            region = BambuCloudRegion.fromStoredValue(prefs.getString(KEY_REGION, null))
        )
    }

    override fun saveSession(session: BambuCloudSession) {
        prefs.edit()
            .putString(KEY_REGION, session.region.storedValue)
            .putString(KEY_ACCESS_TOKEN, session.tokens.accessToken)
            .putString(KEY_REFRESH_TOKEN, session.tokens.refreshToken)
            .putInt(KEY_EXPIRES_IN_SECONDS, session.tokens.expiresInSeconds)
            .putString(KEY_LOGIN_TYPE, session.tokens.loginType)
            .putLong(KEY_UID, session.account.uid)
            .putString(KEY_NAME, session.account.name)
            .putString(KEY_HANDLE, session.account.handle)
            .putString(KEY_AVATAR_URL, session.account.avatarUrl)
            .putString(KEY_BIO, session.account.bio)
            .putLong(KEY_SAVED_AT_MS, session.savedAtMillis)
            .putLong(KEY_EXPIRES_AT_MS, session.expiresAtMillis)
            .apply()
    }

    override fun clearSession() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val PREFS_NAME = "bambu_cloud_session"
        const val KEY_REGION = "region"
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_EXPIRES_IN_SECONDS = "expires_in_seconds"
        const val KEY_LOGIN_TYPE = "login_type"
        const val KEY_UID = "uid"
        const val KEY_NAME = "name"
        const val KEY_HANDLE = "handle"
        const val KEY_AVATAR_URL = "avatar_url"
        const val KEY_BIO = "bio"
        const val KEY_SAVED_AT_MS = "saved_at_ms"
        const val KEY_EXPIRES_AT_MS = "expires_at_ms"
    }
}
