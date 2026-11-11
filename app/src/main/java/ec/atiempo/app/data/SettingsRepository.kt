package ec.atiempo.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import ec.atiempo.app.domain.DoseTime
import ec.atiempo.app.domain.FontSize
import ec.atiempo.app.domain.PurchaseRules
import ec.atiempo.app.domain.ReminderRules
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

/** Preferences from docs/ESPECIFICACION_TECNICA.md §2. Stored in the app's private datastore/ folder. */
data class Settings(
    val userName: String = "",
    val snoozeMinutes: Int = ReminderRules.DEFAULT_SNOOZE_MINUTES,
    val maxRepeats: Int = ReminderRules.DEFAULT_MAX_REPEATS,
    /** Time of the daily urgent purchase reminder (urgentReminderHour in the spec). */
    val urgentReminderTime: DoseTime = PurchaseRules.DEFAULT_URGENT_REMINDER_TIME,
    val fontSize: FontSize = FontSize.NORMAL,
    val onboardingDone: Boolean = false,
)

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(context: Context) {
    private val store = context.applicationContext.settingsStore

    val settings: Flow<Settings> = store.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs ->
            Settings(
                // Stored as typed (so Ajustes can save on every key); spaces at the ends never count.
                userName = prefs[USER_NAME].orEmpty().trim(),
                snoozeMinutes = ReminderRules.snoozeMinutesOrDefault(prefs[SNOOZE_MINUTES]),
                maxRepeats = prefs[MAX_REPEATS] ?: ReminderRules.DEFAULT_MAX_REPEATS,
                urgentReminderTime = urgentTime(prefs[URGENT_HOUR], prefs[URGENT_MINUTE]),
                fontSize = FontSize.entries.firstOrNull { it.name == prefs[FONT_SCALE] } ?: FontSize.NORMAL,
                onboardingDone = prefs[ONBOARDING_DONE] ?: false,
            )
        }

    suspend fun current(): Settings = settings.first()

    suspend fun setUserName(name: String) {
        store.edit { it[USER_NAME] = name.trimStart().take(MAX_NAME_LENGTH) }
    }

    /** Only the choices offered in Ajustes are stored. */
    suspend fun setSnoozeMinutes(minutes: Int) {
        if (minutes !in ReminderRules.SNOOZE_OPTIONS) return
        store.edit { it[SNOOZE_MINUTES] = minutes }
    }

    suspend fun setUrgentReminderTime(time: DoseTime) {
        store.edit {
            it[URGENT_HOUR] = time.hour
            it[URGENT_MINUTE] = time.minute
        }
    }

    suspend fun setFontSize(size: FontSize) {
        store.edit { it[FONT_SCALE] = size.name }
    }

    suspend fun completeOnboarding() {
        store.edit { it[ONBOARDING_DONE] = true }
    }

    companion object {
        const val MAX_NAME_LENGTH = 40

        /** Falls back to 10:00 when nothing (or something out of range) is stored. */
        private fun urgentTime(hour: Int?, minute: Int?): DoseTime {
            val default = PurchaseRules.DEFAULT_URGENT_REMINDER_TIME
            val h = hour?.takeIf { it in 0..23 } ?: return default
            return DoseTime(h, minute?.takeIf { it in 0..59 } ?: 0)
        }

        private val USER_NAME = stringPreferencesKey("userName")
        private val SNOOZE_MINUTES = intPreferencesKey("snoozeMinutes")
        private val MAX_REPEATS = intPreferencesKey("maxRepeats")
        private val URGENT_HOUR = intPreferencesKey("urgentReminderHour")
        private val URGENT_MINUTE = intPreferencesKey("urgentReminderMinute")
        private val FONT_SCALE = stringPreferencesKey("fontScale")
        private val ONBOARDING_DONE = booleanPreferencesKey("onboardingDone")
    }
}
