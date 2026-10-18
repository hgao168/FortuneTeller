package ai.movenova.fortuneteller.data

import androidx.annotation.StringRes
import ai.movenova.fortuneteller.R
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Mirrors iOS ReadingScope — apiValue is the backend contract. */
enum class ReadingScope(val apiValue: String, @param:StringRes val labelRes: Int) {
    TODAY("today", R.string.scope_today),
    MONTH("month", R.string.scope_month),
    YEAR("year", R.string.scope_year),
    LONG_TERM("long_term", R.string.scope_long_term);

    companion object {
        fun fromApi(value: String): ReadingScope =
            entries.firstOrNull { it.apiValue == value } ?: LONG_TERM
    }
}

/** Mirrors iOS MatchType. */
enum class MatchType(val apiValue: String, @param:StringRes val labelRes: Int) {
    ROMANTIC("romantic", R.string.match_type_romantic),
    FRIEND("friend", R.string.match_type_friend);

    companion object {
        fun fromApi(value: String): MatchType =
            entries.firstOrNull { it.apiValue == value } ?: ROMANTIC
    }
}

@Serializable
data class PalmReadingSection(
    val title: String = "",
    val text: String = ""
)

@Serializable
data class PalmReadingResponse(
    val scope: String = "",
    val summary: String = "",
    val sections: List<PalmReadingSection> = emptyList(),
    val advice: List<String> = emptyList(),
    val disclaimer: String = ""
)

@Serializable
data class PalmMatchResponse(
    @SerialName("match_type") val matchType: String = "",
    val score: Int = 0,
    val summary: String = "",
    val strengths: List<String> = emptyList(),
    val tensions: List<String> = emptyList(),
    val advice: List<String> = emptyList(),
    val disclaimer: String = ""
)

/** Local history entry. Thumbnails are JPEG base64 so the whole list persists as one JSON file. */
@Serializable
data class SavedReading(
    val id: String,
    val createdAt: Long,
    val kind: String,
    val scope: String = ReadingScope.LONG_TERM.apiValue,
    val matchType: String = "",
    val reading: PalmReadingResponse? = null,
    val match: PalmMatchResponse? = null,
    val imageBase64: String? = null,
    val imageBBase64: String? = null
) {
    val isMatch: Boolean get() = kind == KIND_MATCH
    val scoreOrNull: Int? get() = match?.score?.coerceIn(0, 100)
    val titleOrSummary: String
        get() = reading?.summary ?: match?.summary ?: ""

    companion object {
        const val KIND_READING = "reading"
        const val KIND_MATCH = "match"
    }
}
