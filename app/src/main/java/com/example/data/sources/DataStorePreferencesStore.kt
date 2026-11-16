package com.example.data.sources

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.LiveChannel
import com.example.data.SportCategory
import com.example.data.abstraction.ThemeMode
import com.example.data.abstraction.UserPreferencesStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

private val Context.arenaDataStore: DataStore<Preferences> by preferencesDataStore(name = "pulse_arena_prefs")

class DataStorePreferencesStore(
    private val context: Context
) : UserPreferencesStore {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val AUTO_SWITCH = booleanPreferencesKey("auto_switch_on_error")
        val PREFER_HTTPS = booleanPreferencesKey("prefer_https_only")
        val SCORE_HUD = booleanPreferencesKey("show_score_hud")
        val CUSTOM_CHANNELS = stringPreferencesKey("custom_channels_json")
    }

    private val _themeMode = MutableStateFlow(ThemeMode.DARK)
    override val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _autoSwitchOnError = MutableStateFlow(true)
    override val autoSwitchOnError: StateFlow<Boolean> = _autoSwitchOnError.asStateFlow()

    private val _preferHttpsOnly = MutableStateFlow(false)
    override val preferHttpsOnly: StateFlow<Boolean> = _preferHttpsOnly.asStateFlow()

    private val _showScoreHudOverlay = MutableStateFlow(true)
    override val showScoreHudOverlay: StateFlow<Boolean> = _showScoreHudOverlay.asStateFlow()

    private val _customChannels = MutableStateFlow<List<LiveChannel>>(emptyList())
    override val customChannels: StateFlow<List<LiveChannel>> = _customChannels.asStateFlow()

    init {
        scope.launch {
            context.arenaDataStore.data.collectLatest { prefs ->
                _themeMode.value = prefs[Keys.THEME_MODE]
                    ?.let { raw -> ThemeMode.entries.firstOrNull { it.name == raw } }
                    ?: ThemeMode.DARK
                _autoSwitchOnError.value = prefs[Keys.AUTO_SWITCH] ?: true
                _preferHttpsOnly.value = prefs[Keys.PREFER_HTTPS] ?: false
                _showScoreHudOverlay.value = prefs[Keys.SCORE_HUD] ?: true
                _customChannels.value = decodeCustomChannels(prefs[Keys.CUSTOM_CHANNELS])
            }
        }
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        context.arenaDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    override suspend fun toggleDarkLight(currentlyDark: Boolean) {
        val next = if (currentlyDark) ThemeMode.LIGHT else ThemeMode.DARK
        setThemeMode(next)
    }

    override suspend fun setAutoSwitchOnError(enabled: Boolean) {
        _autoSwitchOnError.value = enabled
        context.arenaDataStore.edit { it[Keys.AUTO_SWITCH] = enabled }
    }

    override suspend fun setPreferHttpsOnly(enabled: Boolean) {
        _preferHttpsOnly.value = enabled
        context.arenaDataStore.edit { it[Keys.PREFER_HTTPS] = enabled }
    }

    override suspend fun setShowScoreHudOverlay(enabled: Boolean) {
        _showScoreHudOverlay.value = enabled
        context.arenaDataStore.edit { it[Keys.SCORE_HUD] = enabled }
    }

    override suspend fun addCustomChannel(channel: LiveChannel) {
        val updated = listOf(channel) + _customChannels.value
        _customChannels.value = updated
        context.arenaDataStore.edit {
            it[Keys.CUSTOM_CHANNELS] = encodeCustomChannels(updated)
        }
    }

    private fun encodeCustomChannels(list: List<LiveChannel>): String {
        val arr = JSONArray()
        list.take(20).forEach { ch ->
            arr.put(
                JSONObject().apply {
                    put("id", ch.id)
                    put("name", ch.name)
                    put("streamUrl", ch.streamUrl)
                    put("sport", ch.sport.name)
                    put("isHttps", ch.isHttps)
                }
            )
        }
        return arr.toString()
    }

    private fun decodeCustomChannels(json: String?): List<LiveChannel> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(json)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val streamUrl = obj.optString("streamUrl").takeIf { it.isNotBlank() } ?: continue
                    val sport = SportCategory.entries.firstOrNull { it.name == obj.optString("sport") }
                        ?: SportCategory.ALL
                    add(
                        LiveChannel(
                            id = obj.optString("id", "custom_$i"),
                            name = obj.optString("name", "Custom Stream"),
                            streamUrl = streamUrl,
                            logoUrl = null,
                            groupTitle = "Custom URL",
                            sport = sport,
                            isHttps = obj.optBoolean("isHttps", streamUrl.startsWith("https://", true)),
                            qualityLabel = "CUSTOM"
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }
}
