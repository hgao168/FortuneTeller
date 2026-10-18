package ai.movenova.fortuneteller

import ai.movenova.fortuneteller.data.MatchType
import ai.movenova.fortuneteller.data.PalmRequests
import ai.movenova.fortuneteller.data.ReadingScope
import ai.movenova.fortuneteller.data.UploadLimits
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the wire contract to backend/app/main.py. If a field name drifts, this fails instead of the
 * API answering 422 in production (QA defect D9).
 */
class RequestContractTest {

    private val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())

    @Test
    fun analyzeFormUsesBackendFieldNames() {
        val body = PalmRequests.analyzeForm(jpeg, ReadingScope.MONTH, "zh-Hans", "u-1")
            .build()
            .decodeToString()

        assertTrue(body.contains("name=\"image\"; filename=\"palm.jpg\""))
        assertTrue(body.contains("name=\"scope\""))
        assertTrue(body.contains("name=\"language\""))
        assertTrue(body.contains("name=\"user_id\""))
        assertTrue(body.contains("month"))
        assertTrue(body.contains("zh-Hans"))
    }

    @Test
    fun analyzeFormOmitsBlankOptionalFields() {
        val body = PalmRequests.analyzeForm(jpeg, ReadingScope.TODAY, "", "")
            .build()
            .decodeToString()

        assertTrue(body.contains("name=\"scope\""))
        assertFalse(body.contains("name=\"language\""))
        assertFalse(body.contains("name=\"user_id\""))
        assertTrue(body.contains("today"))
    }

    @Test
    fun matchFormUsesBackendFieldNames() {
        val body = PalmRequests.matchForm(
            imageAJpeg = jpeg,
            imageBJpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFE.toByte()),
            matchType = MatchType.FRIEND,
            personABirth = "1990-01-01",
            personBBirth = "1992-02-02",
            language = "en"
        ).build().decodeToString()

        assertTrue(body.contains("name=\"image_a\"; filename=\"palm_a.jpg\""))
        assertTrue(body.contains("name=\"image_b\"; filename=\"palm_b.jpg\""))
        assertTrue(body.contains("name=\"match_type\""))
        assertTrue(body.contains("name=\"person_a_birth\""))
        assertTrue(body.contains("name=\"person_b_birth\""))
        assertTrue(body.contains("friend"))
        assertTrue(body.contains("1990-01-01"))
        assertTrue(body.contains("1992-02-02"))
    }

    @Test
    fun uploadCapMatchesBackendLimit() {
        assertTrue(UploadLimits.MAX_IMAGE_BYTES == 8 * 1024 * 1024)
        assertFalse(UploadLimits.exceedsCap(ByteArray(UploadLimits.MAX_IMAGE_BYTES)))
        assertTrue(UploadLimits.exceedsCap(ByteArray(UploadLimits.MAX_IMAGE_BYTES + 1)))
    }
}
