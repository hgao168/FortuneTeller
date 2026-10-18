package ai.movenova.fortuneteller.data

import java.io.ByteArrayOutputStream
import java.util.UUID

/**
 * Hand-built multipart/form-data encoder.
 *
 * Field names and ordering are the backend contract (see backend/app/main.py), so this is
 * deliberately byte-exact and free of any HTTP framework: the result is fed straight into
 * OkHttp's RequestBody by [ApiClient] and asserted verbatim in unit tests.
 */
class MultipartFormData(val boundary: String = "FortuneTellerBoundary-${UUID.randomUUID()}") {

    private sealed interface Part {
        val name: String
    }

    private class FieldPart(override val name: String, val value: String) : Part

    private class FilePart(
        override val name: String,
        val fileName: String,
        val mimeType: String,
        val bytes: ByteArray
    ) : Part

    private val parts = mutableListOf<Part>()

    fun addField(name: String, value: String): MultipartFormData = apply {
        parts += FieldPart(name, value)
    }

    fun addFile(name: String, fileName: String, mimeType: String, bytes: ByteArray): MultipartFormData = apply {
        parts += FilePart(name, fileName, mimeType, bytes)
    }

    val contentType: String
        get() = "multipart/form-data; boundary=$boundary"

    fun build(): ByteArray {
        val out = ByteArrayOutputStream()
        for (part in parts) {
            out.write("--$boundary\r\n".toByteArray(Charsets.UTF_8))
            when (part) {
                is FieldPart -> {
                    out.write("Content-Disposition: form-data; name=\"${part.name}\"\r\n".toByteArray(Charsets.UTF_8))
                    out.write("Content-Type: text/plain; charset=UTF-8\r\n\r\n".toByteArray(Charsets.UTF_8))
                    out.write(part.value.toByteArray(Charsets.UTF_8))
                    out.write(CRLF)
                }
                is FilePart -> {
                    out.write(
                        "Content-Disposition: form-data; name=\"${part.name}\"; filename=\"${part.fileName}\"\r\n"
                            .toByteArray(Charsets.UTF_8)
                    )
                    out.write("Content-Type: ${part.mimeType}\r\n\r\n".toByteArray(Charsets.UTF_8))
                    out.write(part.bytes)
                    out.write(CRLF)
                }
            }
        }
        out.write("--$boundary--\r\n".toByteArray(Charsets.UTF_8))
        return out.toByteArray()
    }

    private companion object {
        val CRLF = "\r\n".toByteArray(Charsets.UTF_8)
    }
}
