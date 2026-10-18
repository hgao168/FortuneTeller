package ai.movenova.fortuneteller.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/** Mirrors iOS AppLanguage (system / english / chinese). */
enum class AppLanguage(val apiCode: String?, val localeTag: String?) {
    SYSTEM(null, null),
    ENGLISH("en", "en"),
    CHINESE("zh-Hans", "zh-Hans");

    companion object {
        fun fromStorage(value: String?): AppLanguage =
            entries.firstOrNull { it.name == value } ?: SYSTEM
    }
}

/**
 * Language preference + the context used to resolve strings.
 *
 * iOS swaps bundles at runtime; on Android we resolve strings through a Context whose
 * configuration carries the chosen locale, so a language change re-renders without restarting
 * the activity.
 */
class AppSettings(context: Context) {

    private val prefs = context.getSharedPreferences("fortune_settings", Context.MODE_PRIVATE)
    private val _language = MutableStateFlow(AppLanguage.fromStorage(prefs.getString(KEY_LANGUAGE, null)))
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    fun setLanguage(language: AppLanguage) {
        _language.value = language
        prefs.edit().putString(KEY_LANGUAGE, language.name).apply()
    }

    /** API language code: explicit choice wins, otherwise the device locale decides. */
    fun apiLanguageCode(): String {
        _language.value.apiCode?.let { return it }
        val preferred = Locale.getDefault().language.lowercase(Locale.ROOT)
        return if (preferred.startsWith("zh")) "zh-Hans" else "en"
    }

    /** Base context reconfigured for the chosen language (system language when unset). */
    fun localizedContext(base: Context): Context {
        val tag = _language.value.localeTag ?: return base
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val config = android.content.res.Configuration(base.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return base.createConfigurationContext(config)
    }

    private companion object {
        const val KEY_LANGUAGE = "app.language"
    }
}
