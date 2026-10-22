package com.example.onlinelearning.data.model

/** Course as served by the Retrofit catalog API (see hosting/public/api/courses.json). */
data class Course(
    val id: String = "",
    val title: String = "",
    val author: String = "",
    val category: String = "",
    val price: Double = 0.0,
    val durationMinutes: Int = 0,
    val imageUrl: String = "",
    val description: String = "",
    /** Where the lesson recordings come from (shown as a credit on the course screen). */
    val sourceName: String = "",
    val sourceUrl: String = "",
    val lessons: List<Lesson> = emptyList(),
) {
    val durationLabel: String
        get() = "${durationMinutes / 60}h ${durationMinutes % 60}min"

    val priceLabel: String
        get() = "$" + "%.2f".format(price)
}

data class Lesson(
    val id: String = "",
    val title: String = "",
    val durationMinutes: Int = 0,
    /** Streamed audio (mp3/ogg); takes priority over the other sources. */
    val audioUrl: String = "",
    /** Streamed video (mp4), played in the in-app player. */
    val videoUrl: String = "",
    /** Lesson script read aloud on the device with TextToSpeech when there is no recording. */
    val text: String = "",
    /** Free lessons can be played before the course is bought. */
    val free: Boolean = false,
) {
    val kind: LessonKind
        get() = when {
            audioUrl.isNotBlank() -> LessonKind.AUDIO
            videoUrl.isNotBlank() -> LessonKind.VIDEO
            text.isNotBlank() -> LessonKind.NARRATION
            else -> LessonKind.NONE
        }

    /** Listen-only lessons (recorded audio or narration). */
    val isAudio: Boolean
        get() = kind == LessonKind.AUDIO || kind == LessonKind.NARRATION
}

enum class LessonKind { AUDIO, VIDEO, NARRATION, NONE }

/** Per-user state stored in Firestore under users/{uid}. */
data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val favorites: List<String> = emptyList(),
    val purchased: List<String> = emptyList(),
    /** courseId -> ids of completed lessons. */
    val progress: Map<String, List<String>> = emptyMap(),
    val learnedTodayMinutes: Int = 0,
)
