package ai.movenova.fortuneteller

import ai.movenova.fortuneteller.data.MatchType
import ai.movenova.fortuneteller.data.PalmJson
import ai.movenova.fortuneteller.data.ReadingScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Contract tests: backend payloads must decode without loss, extra keys must not break decoding. */
class ModelsTest {

    private val readingJson = """
        {
          "scope": "month",
          "summary": "A grounded period.",
          "sections": [
            {"title": "Career", "text": "Steady growth."},
            {"title": "Relationships", "text": "Open communication."}
          ],
          "advice": ["Keep one habit", "Finish one conversation"],
          "disclaimer": "Entertainment only."
        }
    """.trimIndent()

    private val matchJson = """
        {
          "match_type": "romantic",
          "score": 78,
          "summary": "Warm dynamic.",
          "strengths": ["Trust", "Growth"],
          "tensions": ["Pace"],
          "advice": ["Weekly ritual"],
          "disclaimer": "Entertainment only."
        }
    """.trimIndent()

    @Test
    fun decodesReadingResponse() {
        val response = PalmJson.parseReading(readingJson)
        assertEquals("month", response.scope)
        assertEquals("A grounded period.", response.summary)
        assertEquals(2, response.sections.size)
        assertEquals("Career", response.sections[0].title)
        assertEquals(2, response.advice.size)
        assertEquals("Entertainment only.", response.disclaimer)
    }

    @Test
    fun decodesMatchResponse() {
        val response = PalmJson.parseMatch(matchJson)
        assertEquals("romantic", response.matchType)
        assertEquals(78, response.score)
        assertEquals(2, response.strengths.size)
        assertEquals(1, response.tensions.size)
        assertEquals(1, response.advice.size)
    }

    @Test
    fun toleratesUnknownAndMissingFields() {
        val sparse = """{"score": 42, "summary": "Only a score", "server_only_field": true}"""
        val response = PalmJson.parseMatch(sparse)
        assertEquals(42, response.score)
        assertEquals("Only a score", response.summary)
        assertTrue(response.strengths.isEmpty())
        assertEquals("", response.disclaimer)
    }

    @Test
    fun decodesFencedJsonAfterBackendStripsIt() {
        // The backend strips ``` fences before returning, so the client sees raw JSON.
        val response = PalmJson.parseReading(
            """{"summary":"x","sections":[{"title":"Health","text":"Balance"}],"advice":[]}"""
        )
        assertEquals("x", response.summary)
        assertNotNull(response.sections.firstOrNull())
    }

    @Test
    fun savedReadingClampsOutOfRangeScore() {
        val high = ai.movenova.fortuneteller.data.SavedReading(
            id = "1", createdAt = 0L, kind = ai.movenova.fortuneteller.data.SavedReading.KIND_MATCH,
            match = ai.movenova.fortuneteller.data.PalmMatchResponse(score = 184)
        )
        val low = ai.movenova.fortuneteller.data.SavedReading(
            id = "2", createdAt = 0L, kind = ai.movenova.fortuneteller.data.SavedReading.KIND_MATCH,
            match = ai.movenova.fortuneteller.data.PalmMatchResponse(score = -5)
        )
        assertEquals(100, high.scoreOrNull)
        assertEquals(0, low.scoreOrNull)
    }

    @Test
    fun scopeAndMatchTypeMapFromApiValues() {
        assertEquals(ReadingScope.MONTH, ReadingScope.fromApi("month"))
        assertEquals(ReadingScope.LONG_TERM, ReadingScope.fromApi("long_term"))
        assertEquals(ReadingScope.LONG_TERM, ReadingScope.fromApi("unknown"))
        assertEquals(MatchType.ROMANTIC, MatchType.fromApi("romantic"))
        assertEquals(MatchType.FRIEND, MatchType.fromApi("friend"))
        assertEquals(MatchType.ROMANTIC, MatchType.fromApi("nonsense"))
    }
}
