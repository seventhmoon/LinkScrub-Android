package com.androidfung.linkscrub.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@Immutable
data class UserSettings(
    val isQuickCleanDefault: Boolean = false,
    val unshortenRedirects: Boolean = true,
    val fetchOpenGraphPreview: Boolean = true,
    val customBlocklist: Set<String> = emptySet(),
    val customAllowlist: Set<String> = emptySet()
)

interface SettingsRepository {
    val settings: StateFlow<UserSettings>
    fun setQuickCleanDefault(enabled: Boolean)
    fun setUnshortenRedirects(enabled: Boolean)
    fun setFetchOpenGraphPreview(enabled: Boolean)
    fun addCustomBlockedParam(param: String)
    fun removeCustomBlockedParam(param: String)
    fun addCustomAllowedParam(param: String)
    fun removeCustomAllowedParam(param: String)
}

class InMemorySettingsRepository(
    initialSettings: UserSettings = UserSettings()
) : SettingsRepository {
    private val _settings = MutableStateFlow(initialSettings)
    override val settings: StateFlow<UserSettings> = _settings.asStateFlow()

    override fun setQuickCleanDefault(enabled: Boolean) {
        _settings.update { it.copy(isQuickCleanDefault = enabled) }
    }

    override fun setUnshortenRedirects(enabled: Boolean) {
        _settings.update { it.copy(unshortenRedirects = enabled) }
    }

    override fun setFetchOpenGraphPreview(enabled: Boolean) {
        _settings.update { it.copy(fetchOpenGraphPreview = enabled) }
    }

    override fun addCustomBlockedParam(param: String) {
        val normalized = param.trim().lowercase()
        if (normalized.isEmpty()) return
        _settings.update {
            it.copy(
                customBlocklist = it.customBlocklist + normalized,
                customAllowlist = it.customAllowlist - normalized
            )
        }
    }

    override fun removeCustomBlockedParam(param: String) {
        val normalized = param.trim().lowercase()
        _settings.update { it.copy(customBlocklist = it.customBlocklist - normalized) }
    }

    override fun addCustomAllowedParam(param: String) {
        val normalized = param.trim().lowercase()
        if (normalized.isEmpty()) return
        _settings.update {
            it.copy(
                customAllowlist = it.customAllowlist + normalized,
                customBlocklist = it.customBlocklist - normalized
            )
        }
    }

    override fun removeCustomAllowedParam(param: String) {
        val normalized = param.trim().lowercase()
        _settings.update { it.copy(customAllowlist = it.customAllowlist - normalized) }
    }
}

class SharedPreferencesSettingsRepository(
    context: Context
) : SettingsRepository {

    private val prefs: SharedPreferences? = try {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    } catch (_: Exception) {
        null
    }

    private val _settings = MutableStateFlow(loadFromPrefs())
    override val settings: StateFlow<UserSettings> = _settings.asStateFlow()

    private fun loadFromPrefs(): UserSettings {
        val p = prefs ?: return UserSettings()
        return try {
            UserSettings(
                isQuickCleanDefault = p.getBoolean(KEY_QUICK_CLEAN_DEFAULT, false),
                unshortenRedirects = p.getBoolean(KEY_UNSHORTEN_REDIRECTS, true),
                fetchOpenGraphPreview = p.getBoolean(KEY_FETCH_OG_PREVIEW, true),
                customBlocklist = p.getStringSet(KEY_CUSTOM_BLOCKLIST, emptySet())
                    ?.map { it.trim().lowercase() }
                    ?.filter { it.isNotEmpty() }
                    ?.toSet() ?: emptySet(),
                customAllowlist = p.getStringSet(KEY_CUSTOM_ALLOWLIST, emptySet())
                    ?.map { it.trim().lowercase() }
                    ?.filter { it.isNotEmpty() }
                    ?.toSet() ?: emptySet()
            )
        } catch (_: Exception) {
            UserSettings()
        }
    }

    override fun setQuickCleanDefault(enabled: Boolean) {
        _settings.update { it.copy(isQuickCleanDefault = enabled) }
        prefs?.edit()?.putBoolean(KEY_QUICK_CLEAN_DEFAULT, enabled)?.apply()
    }

    override fun setUnshortenRedirects(enabled: Boolean) {
        _settings.update { it.copy(unshortenRedirects = enabled) }
        prefs?.edit()?.putBoolean(KEY_UNSHORTEN_REDIRECTS, enabled)?.apply()
    }

    override fun setFetchOpenGraphPreview(enabled: Boolean) {
        _settings.update { it.copy(fetchOpenGraphPreview = enabled) }
        prefs?.edit()?.putBoolean(KEY_FETCH_OG_PREVIEW, enabled)?.apply()
    }

    override fun addCustomBlockedParam(param: String) {
        val normalized = param.trim().lowercase()
        if (normalized.isEmpty()) return
        _settings.update { current ->
            val updatedBlock = current.customBlocklist + normalized
            val updatedAllow = current.customAllowlist - normalized
            prefs?.edit()
                ?.putStringSet(KEY_CUSTOM_BLOCKLIST, updatedBlock)
                ?.putStringSet(KEY_CUSTOM_ALLOWLIST, updatedAllow)
                ?.apply()
            current.copy(customBlocklist = updatedBlock, customAllowlist = updatedAllow)
        }
    }

    override fun removeCustomBlockedParam(param: String) {
        val normalized = param.trim().lowercase()
        _settings.update { current ->
            val updatedBlock = current.customBlocklist - normalized
            prefs?.edit()?.putStringSet(KEY_CUSTOM_BLOCKLIST, updatedBlock)?.apply()
            current.copy(customBlocklist = updatedBlock)
        }
    }

    override fun addCustomAllowedParam(param: String) {
        val normalized = param.trim().lowercase()
        if (normalized.isEmpty()) return
        _settings.update { current ->
            val updatedAllow = current.customAllowlist + normalized
            val updatedBlock = current.customBlocklist - normalized
            prefs?.edit()
                ?.putStringSet(KEY_CUSTOM_ALLOWLIST, updatedAllow)
                ?.putStringSet(KEY_CUSTOM_BLOCKLIST, updatedBlock)
                ?.apply()
            current.copy(customAllowlist = updatedAllow, customBlocklist = updatedBlock)
        }
    }

    override fun removeCustomAllowedParam(param: String) {
        val normalized = param.trim().lowercase()
        _settings.update { current ->
            val updatedAllow = current.customAllowlist - normalized
            prefs?.edit()?.putStringSet(KEY_CUSTOM_ALLOWLIST, updatedAllow)?.apply()
            current.copy(customAllowlist = updatedAllow)
        }
    }

    companion object {
        private const val PREFS_NAME = "linkscrub_settings"
        private const val KEY_QUICK_CLEAN_DEFAULT = "quick_clean_default"
        private const val KEY_UNSHORTEN_REDIRECTS = "unshorten_redirects"
        private const val KEY_FETCH_OG_PREVIEW = "fetch_og_preview"
        private const val KEY_CUSTOM_BLOCKLIST = "custom_blocklist"
        private const val KEY_CUSTOM_ALLOWLIST = "custom_allowlist"
    }
}
