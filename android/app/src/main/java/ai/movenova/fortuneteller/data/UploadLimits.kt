package ai.movenova.fortuneteller.data

/** Backend upload cap (see backend/app/main.py: MAX_IMAGE_BYTES = 8 * 1024 * 1024). */
object UploadLimits {
    const val MAX_IMAGE_BYTES: Int = 8 * 1024 * 1024

    fun exceedsCap(bytes: ByteArray): Boolean = bytes.size > MAX_IMAGE_BYTES
}
