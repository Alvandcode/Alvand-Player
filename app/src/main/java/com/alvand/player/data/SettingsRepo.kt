package com.alvand.player.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore("alvand_settings")

/** حالت تم: ۰=سیستم، ۱=روشن، ۲=تیره */
object ThemeMode {
    const val SYSTEM = 0
    const val LIGHT = 1
    const val DARK = 2
}

/**
 * تنظیمات ظاهری اپ (تم + بکگراند دلخواه) با DataStore — بین اجراها می‌ماند.
 */
@Singleton
class SettingsRepo @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val store get() = context.settingsStore

    val themeMode: Flow<Int> = store.data.map { it[Keys.THEME] ?: ThemeMode.SYSTEM }
    val backgroundUri: Flow<String?> = store.data.map { it[Keys.BG] }
    val onboardingSeen: Flow<Boolean> = store.data.map { it[Keys.ONBOARDING] ?: false }

    /** علاقه‌مندی‌های پایدار: Set<String> چون DataStore longSet ندارد */
    val likedIds: Flow<Set<Long>> = store.data.map { prefs ->
        prefs[Keys.LIKED]?.mapNotNull { it.toLongOrNull() }?.toSet() ?: emptySet()
    }
    /** حالت سورت کتابخانه (LibrarySort.*) */
    val librarySort: Flow<Int> = store.data.map { it[Keys.SORT] ?: LibrarySort.DEFAULT }

    suspend fun setThemeMode(mode: Int) {
        store.edit { it[Keys.THEME] = mode.coerceIn(0, 2) }
    }

    suspend fun setBackground(uriString: String?) {
        store.edit {
            if (uriString == null) it.remove(Keys.BG)
            else it[Keys.BG] = uriString
        }
    }

    suspend fun setOnboardingSeen(seen: Boolean = true) {
        store.edit { it[Keys.ONBOARDING] = seen }
    }

    suspend fun toggleLike(id: Long) {
        store.edit { prefs ->
            val cur = prefs[Keys.LIKED] ?: emptySet()
            val key = id.toString()
            prefs[Keys.LIKED] = if (key in cur) cur - key else cur + key
        }
    }

    suspend fun setLibrarySort(mode: Int) {
        store.edit { it[Keys.SORT] = mode.coerceIn(0, 4) }
    }

    private object Keys {
        val THEME = intPreferencesKey("theme_mode")
        val BG = stringPreferencesKey("background_uri")
        val ONBOARDING = androidx.datastore.preferences.core.booleanPreferencesKey("onboarding_seen")
        val LIKED = stringSetPreferencesKey("liked_ids")
        val SORT = intPreferencesKey("library_sort")
    }
}
