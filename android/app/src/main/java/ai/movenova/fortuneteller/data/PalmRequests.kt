package ai.movenova.fortuneteller.data

/**
 * Multipart payload builders for the two backend endpoints.
 *
 * Field names are the contract from backend/app/main.py. They live in one place so unit tests can
 * pin them: renaming a field here without touching the backend must fail a test, not silently 422
 * in production.
 */
object PalmRequests {

    const val FIELD_IMAGE = "image"
    const val FIELD_SCOPE = "scope"
    const val FIELD_LANGUAGE = "language"
    const val FIELD_USER_ID = "user_id"

    const val FIELD_IMAGE_A = "image_a"
    const val FIELD_IMAGE_B = "image_b"
    const val FIELD_MATCH_TYPE = "match_type"
    const val FIELD_PERSON_A_BIRTH = "person_a_birth"
    const val FIELD_PERSON_B_BIRTH = "person_b_birth"

    fun analyzeForm(
        imageJpeg: ByteArray,
        scope: ReadingScope,
        language: String?,
        userId: String? = null
    ): MultipartFormData {
        val form = MultipartFormData()
            .addFile(FIELD_IMAGE, "palm.jpg", "image/jpeg", imageJpeg)
            .addField(FIELD_SCOPE, scope.apiValue)
        language?.takeIf { it.isNotBlank() }?.let { form.addField(FIELD_LANGUAGE, it) }
        userId?.takeIf { it.isNotBlank() }?.let { form.addField(FIELD_USER_ID, it) }
        return form
    }

    fun matchForm(
        imageAJpeg: ByteArray,
        imageBJpeg: ByteArray,
        matchType: MatchType,
        personABirth: String,
        personBBirth: String,
        language: String?
    ): MultipartFormData {
        val form = MultipartFormData()
            .addFile(FIELD_IMAGE_A, "palm_a.jpg", "image/jpeg", imageAJpeg)
            .addFile(FIELD_IMAGE_B, "palm_b.jpg", "image/jpeg", imageBJpeg)
            .addField(FIELD_MATCH_TYPE, matchType.apiValue)
            .addField(FIELD_PERSON_A_BIRTH, personABirth)
            .addField(FIELD_PERSON_B_BIRTH, personBBirth)
        language?.takeIf { it.isNotBlank() }?.let { form.addField(FIELD_LANGUAGE, it) }
        return form
    }
}
