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
    /** Audio lessons play inside the app; takes priority over [videoUrl]. */
    val audioUrl: String = "",
    /** Legacy video lessons are opened in an external player. */
    val videoUrl: String = "",
    /** Free lessons can be played before the course is bought. */
    val free: Boolean = false,
) {
    val isAudio: Boolean
        get() = audioUrl.isNotBlank()
}

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
