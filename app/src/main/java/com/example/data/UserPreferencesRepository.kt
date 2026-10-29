package com.example.data

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.R
import com.example.ui.theme.PixelThemeType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "pixel_rotina_prefs")

enum class AmbientSceneEffect {
    SUN_MOTES,
    RAIN_DROPS,
    TWINKLING_STARS
}

enum class HeroArtPreference(
    val id: String,
    val title: String,
    val subtitle: String,
    val greeting: String,
    val cardSubtitle: String,
    @DrawableRes val drawableRes: Int?,
    val ambientEffect: AmbientSceneEffect = AmbientSceneEffect.SUN_MOTES
) {
    AUTO(
        id = "AUTO",
        title = "🔄 Automático (Com Variações do Dia)",
        subtitle = "Muda conforme o horário e alterna variações de cenário",
        greeting = "SUA ROTINA DE HOJE",
        cardSubtitle = "No seu ritmo, com leveza e constância",
        drawableRes = null,
        ambientEffect = AmbientSceneEffect.SUN_MOTES
    ),
    MORNING(
        id = "MORNING",
        title = "☀️ Manhã Ensolarada (Mesa & Plantas)",
        subtitle = "Mesa iluminada com plantas, água fresca e sol da manhã",
        greeting = "BOM DIA • SUA ROTINA DE HOJE",
        cardSubtitle = "Comece com leveza, água fresca e luz da manhã",
        drawableRes = R.drawable.img_pixel_hero_banner,
        ambientEffect = AmbientSceneEffect.SUN_MOTES
    ),
    MORNING_RAIN(
        id = "MORNING_RAIN",
        title = "🌧️ Manhã de Chuva Calma & Café",
        subtitle = "Chuvinha relaxante na janela, café quente e broto verde",
        greeting = "BOM DIA • MANHÃ ACONCHEGANTE",
        cardSubtitle = "Dia calmo para focar e cuidar do seu bem-estar",
        drawableRes = R.drawable.img_pixel_hero_rainy_morning,
        ambientEffect = AmbientSceneEffect.RAIN_DROPS
    ),
    AFTERNOON(
        id = "AFTERNOON",
        title = "🌤️ Tarde Lo-Fi (Chá & Gatinho)",
        subtitle = "Luz dourada da tarde, chá gelado e gatinho descansando",
        greeting = "BOA TARDE • SUA ROTINA DE HOJE",
        cardSubtitle = "Mantenha seu ritmo com pausas e hidratação",
        drawableRes = R.drawable.img_pixel_hero_afternoon,
        ambientEffect = AmbientSceneEffect.SUN_MOTES
    ),
    AFTERNOON_GARDEN(
        id = "AFTERNOON_GARDEN",
        title = "🌿 Tarde no Jardim & Estufa Pixel",
        subtitle = "Varanda florida à tarde com jarra de água fresca e plantas",
        greeting = "BOA TARDE • AR FRESCO & ENERGIA",
        cardSubtitle = "Respire fundo, hidrate-se e cuide do corpo",
        drawableRes = R.drawable.img_pixel_hero_garden_afternoon,
        ambientEffect = AmbientSceneEffect.SUN_MOTES
    ),
    EVENING(
        id = "EVENING",
        title = "🌇 Pôr do Sol & Crepúsculo",
        subtitle = "Varanda ao entardecer com luzes quentes e céu lilás/coral",
        greeting = "FIM DE TARDE • SUA ROTINA DE HOJE",
        cardSubtitle = "Desacelere aos poucos e cuide de você",
        drawableRes = R.drawable.img_pixel_hero_evening,
        ambientEffect = AmbientSceneEffect.TWINKLING_STARS
    ),
    NIGHT(
        id = "NIGHT",
        title = "🌙 Noite Estrelada & Luminária",
        subtitle = "Quarto tranquilo à noite com luminária quente e céu estrelado",
        greeting = "BOA NOITE • SUA ROTINA DE HOJE",
        cardSubtitle = "Ambiente calmo para encerrar o dia em paz",
        drawableRes = R.drawable.img_pixel_hero_night,
        ambientEffect = AmbientSceneEffect.TWINKLING_STARS
    ),
    NIGHT_CATPPUCCIN(
        id = "NIGHT_CATPPUCCIN",
        title = "🐾 Noite Catppuccin (Gatinho & Luzes)",
        subtitle = "Estúdio noturno em tons Mocha com gatinho dormindo e estrelas",
        greeting = "BOA NOITE • DESCANSO MERECIDO",
        cardSubtitle = "Prepare-se para uma noite tranquila e revigorante",
        drawableRes = R.drawable.img_pixel_hero_cozy_cat_night,
        ambientEffect = AmbientSceneEffect.TWINKLING_STARS
    );

    fun resolveForHour(
        hourOfDay: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
        variationIndex: Int = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
    ): HeroArtPreference {
        if (this != AUTO) return this
        val isAltVariant = variationIndex % 2 == 1
        return when (hourOfDay) {
            in 5..11 -> if (isAltVariant) MORNING_RAIN else MORNING
            in 12..16 -> if (isAltVariant) AFTERNOON_GARDEN else AFTERNOON
            in 17..19 -> EVENING
            else -> if (isAltVariant) NIGHT_CATPPUCCIN else NIGHT
        }
    }

    companion object {
        fun fromId(id: String?): HeroArtPreference {
            return entries.firstOrNull { it.id == id } ?: AUTO
        }
    }
}

data class UserSettings(
    val themeType: PixelThemeType = PixelThemeType.CATPPUCCIN_MOCHA,
    val heroArtPreference: HeroArtPreference = HeroArtPreference.AUTO,
    val defaultSnoozeMinutes: Int = 15,
    val alarmSnoozeMinutes: Int = 10,
    val vibrationEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val hasSeededDefaults: Boolean = false,
    val hasCompletedOnboarding: Boolean = false,
    val lastBackupTimestamp: Long = 0L
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val THEME_ID = stringPreferencesKey("theme_id")
        val HERO_ART_ID = stringPreferencesKey("hero_art_id")
        val DEFAULT_SNOOZE_MIN = intPreferencesKey("default_snooze_min")
        val ALARM_SNOOZE_MIN = intPreferencesKey("alarm_snooze_min")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val HAS_SEEDED_DEFAULTS = booleanPreferencesKey("has_seeded_defaults")
        val HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("has_completed_onboarding")
        val LAST_BACKUP_TIMESTAMP = longPreferencesKey("last_backup_timestamp")
    }

    val settingsFlow: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        UserSettings(
            themeType = PixelThemeType.fromId(prefs[Keys.THEME_ID]),
            heroArtPreference = HeroArtPreference.fromId(prefs[Keys.HERO_ART_ID]),
            defaultSnoozeMinutes = prefs[Keys.DEFAULT_SNOOZE_MIN] ?: 15,
            alarmSnoozeMinutes = prefs[Keys.ALARM_SNOOZE_MIN] ?: 10,
            vibrationEnabled = prefs[Keys.VIBRATION_ENABLED] ?: true,
            soundEnabled = prefs[Keys.SOUND_ENABLED] ?: true,
            hasSeededDefaults = prefs[Keys.HAS_SEEDED_DEFAULTS] ?: false,
            hasCompletedOnboarding = prefs[Keys.HAS_COMPLETED_ONBOARDING] ?: false,
            lastBackupTimestamp = prefs[Keys.LAST_BACKUP_TIMESTAMP] ?: 0L
        )
    }

    suspend fun setTheme(themeType: PixelThemeType) {
        context.dataStore.edit { prefs ->
            prefs[Keys.THEME_ID] = themeType.id
        }
    }

    suspend fun setHeroArtPreference(preference: HeroArtPreference) {
        context.dataStore.edit { prefs ->
            prefs[Keys.HERO_ART_ID] = preference.id
        }
    }

    suspend fun setDefaultSnoozeMinutes(minutes: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DEFAULT_SNOOZE_MIN] = minutes.coerceIn(5, 120)
        }
    }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.VIBRATION_ENABLED] = enabled
        }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SOUND_ENABLED] = enabled
        }
    }

    suspend fun markSeededDefaults() {
        context.dataStore.edit { prefs ->
            prefs[Keys.HAS_SEEDED_DEFAULTS] = true
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean = true) {
        context.dataStore.edit { prefs ->
            prefs[Keys.HAS_COMPLETED_ONBOARDING] = completed
        }
    }

    suspend fun recordBackupTimestamp(timestamp: Long = System.currentTimeMillis()) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAST_BACKUP_TIMESTAMP] = timestamp
        }
    }
}
