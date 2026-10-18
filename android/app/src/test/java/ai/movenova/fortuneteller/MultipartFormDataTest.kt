package ai.movenova.fortuneteller

import ai.movenova.fortuneteller.data.MultipartFormData
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Mirrors FortuneTellerTests/MultipartFormDataTests.swift — same six acceptance cases. */
class MultipartFormDataTest {

    @Test
    fun contentTypeIncludesBoundary() {
        val form = MultipartFormData(boundary = "BoundaryUnitTest")
        assertEquals("multipart/form-data; boundary=BoundaryUnitTest", form.contentType)
    }

    @Test
    fun defaultBoundaryIsUnique() {
        val first = MultipartFormData()
        val second = MultipartFormData()
        assertNotEquals(first.boundary, second.boundary)
        assertTrue(first.boundary.startsWith("FortuneTellerBoundary-"))
    }

    @Test
    fun fieldEncoding() {
        val body = MultipartFormData(boundary = "B").addField("scope", "month").build().decodeToString()
        assertTrue(body.startsWith("--B\r\n"))
        assertTrue(body.contains("Content-Disposition: form-data; name=\"scope\""))
        assertTrue(body.contains("Content-Type: text/plain; charset=UTF-8"))
        assertTrue(body.contains("\r\n\r\nmonth\r\n"))
        assertTrue(body.endsWith("--B--\r\n"))
    }

    @Test
    fun fileEncodingHeadersAndBytes() {
        val payload = byteArrayOf(0x01, 0x02, 0x03)
        val bytes = MultipartFormData(boundary = "B")
            .addFile("image", "palm.jpg", "image/jpeg", payload)
            .build()

        val text = bytes.decodeToString()
        assertTrue(text.contains("Content-Disposition: form-data; name=\"image\"; filename=\"palm.jpg\""))
        assertTrue(text.contains("Content-Type: image/jpeg"))

        // Raw payload bytes must be present verbatim, not string-mangled.
        val marker = "Content-Type: image/jpeg\r\n\r\n".toByteArray()
        val start = bytes.indexOfSubArray(marker) + marker.size
        assertArrayEquals(payload, bytes.copyOfRange(start, start + payload.size))
    }

    @Test
    fun multipleFieldsOrderingPreserved() {
        val body = MultipartFormData(boundary = "B")
            .addField("match_type", "romantic")
            .addField("person_a_birth", "1990-01-01")
            .addField("person_b_birth", "1992-02-02")
            .build()
            .decodeToString()

        val first = body.indexOf("name=\"match_type\"")
        val second = body.indexOf("name=\"person_a_birth\"")
        val third = body.indexOf("name=\"person_b_birth\"")
        assertTrue(first in 0 until second)
        assertTrue(second in 0 until third)
    }

    @Test
    fun customMimeType() {
        val text = MultipartFormData(boundary = "B")
            .addFile("image", "palm.png", "image/png", byteArrayOf(0x0A))
            .build()
            .decodeToString()
        assertTrue(text.contains("Content-Type: image/png"))
        assertTrue(text.contains("filename=\"palm.png\""))
    }

    private fun ByteArray.indexOfSubArray(needle: ByteArray): Int {
        if (needle.isEmpty() || needle.size > size) return -1
        outer@ for (i in 0..size - needle.size) {
            for (j in needle.indices) {
                if (this[i + j] != needle[j]) continue@outer
            }
            return i
        }
        return -1
    }
}
