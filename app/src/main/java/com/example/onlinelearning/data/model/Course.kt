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
    /** Catalog labels: "bestseller", "popular", "new". */
    val tags: List<String> = emptyList(),
    val lessons: List<Lesson> = emptyList(),
) {
    val isBestseller: Boolean get() = "bestseller" in tags
    val isPopular: Boolean get() = "popular" in tags
    val isNew: Boolean get() = "new" in tags

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
    /** Legacy counter (never reset); kept so old documents still load. Use [minutesOn] instead. */
    val learnedTodayMinutes: Int = 0,
    /** Profile photo: 256x256 JPEG encoded as Base64 (see AvatarCodec); empty when not set. */
    val avatar: String = "",
    /** ISO date (yyyy-MM-dd) -> minutes learned that day. */
    val activity: Map<String, Long> = emptyMap(),
    /** Minutes learned over all time. */
    val totalMinutes: Long = 0,
    /** Messages and notifications shown on the Message tab, newest last. */
    val notifications: List<AppNotification> = emptyList(),
) {
    fun minutesOn(date: java.time.LocalDate): Int = (activity[date.toString()] ?: 0L).toInt()
}

/** One item of the Message tab. [kind] is "message" (from the team) or "notification" (an event). */
data class AppNotification(
    val kind: String = KIND_NOTIFICATION,
    /** "purchase", "lesson", "course", "welcome" — picks the icon. */
    val type: String = "",
    val title: String = "",
    val text: String = "",
    val time: Long = 0,
) {
    companion object {
        const val KIND_MESSAGE = "message"
        const val KIND_NOTIFICATION = "notification"
    }
}
