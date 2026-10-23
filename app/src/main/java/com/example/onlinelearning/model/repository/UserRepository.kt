package com.example.onlinelearning.model.repository

import com.example.onlinelearning.model.entity.AppNotification
import com.example.onlinelearning.model.entity.Course
import com.example.onlinelearning.model.entity.Lesson
import com.example.onlinelearning.model.entity.UserProfile
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import java.time.LocalDate

/**
 * Per-user data in Cloud Firestore, document users/{uid}:
 * favorites, purchased courses and lesson progress.
 */
class UserRepository(private val db: FirebaseFirestore?) {

    fun profileFlow(uid: String?): Flow<UserProfile?> {
        if (db == null || uid == null) return flowOf(null)
        return callbackFlow {
            val registration = userDoc(uid).addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObject(UserProfile::class.java)?.copy(uid = uid))
            }
            awaitClose { registration.remove() }
        }
    }

    suspend fun createProfile(uid: String, name: String, email: String) {
        val data = mapOf("uid" to uid, "name" to name, "email" to email)
        userDoc(uid).set(data, SetOptions.merge()).await()
    }

    /**
     * Creates users/{uid} if it is missing, e.g. when sign-up created the Auth account but the
     * Firestore write failed. Never overwrites an existing name with a blank one.
     */
    suspend fun ensureProfile(uid: String, name: String?, email: String?) {
        val doc = userDoc(uid)
        if (doc.get().await().exists()) return
        val data = buildMap<String, Any> {
            put("uid", uid)
            if (!email.isNullOrBlank()) put("email", email)
            if (!name.isNullOrBlank()) put("name", name)
            put("notifications", FieldValue.arrayUnion(welcomeMessage()))
        }
        doc.set(data, SetOptions.merge()).await()
    }

    suspend fun updateName(uid: String, name: String) {
        userDoc(uid).set(mapOf("name" to name), SetOptions.merge()).await()
    }

    /** Saves the Base64 avatar, or removes it when [avatar] is null. */
    suspend fun setAvatar(uid: String, avatar: String?) {
        userDoc(uid).set(mapOf("avatar" to (avatar ?: FieldValue.delete())), SetOptions.merge()).await()
    }

    suspend fun setFavorite(uid: String, courseId: String, favorite: Boolean) {
        val value = if (favorite) FieldValue.arrayUnion(courseId) else FieldValue.arrayRemove(courseId)
        userDoc(uid).set(mapOf("favorites" to value), SetOptions.merge()).await()
    }

    /** Demo checkout: records the purchase without charging anything. */
    suspend fun purchase(uid: String, courseId: String, courseTitle: String) {
        val note = notification(
            kind = AppNotification.KIND_NOTIFICATION,
            type = "purchase",
            title = "Successful purchase!",
            text = "Course «$courseTitle» is now in My courses",
        )
        userDoc(uid).set(
            mapOf("purchased" to FieldValue.arrayUnion(courseId), "notifications" to FieldValue.arrayUnion(note)),
            SetOptions.merge(),
        ).await()
    }

    /**
     * Marks a lesson as done and adds its minutes to today's activity and the all-time total.
     * [notes] are optional Message-tab entries (first completion, finished course).
     */
    suspend fun completeLesson(
        uid: String,
        courseId: String,
        lessonId: String,
        minutes: Int,
        notes: List<Map<String, Any>> = emptyList(),
    ) {
        val today = LocalDate.now().toString()
        val data = buildMap<String, Any> {
            put("progress", mapOf(courseId to FieldValue.arrayUnion(lessonId)))
            put("activity", mapOf(today to FieldValue.increment(minutes.toLong())))
            put("totalMinutes", FieldValue.increment(minutes.toLong()))
            if (notes.isNotEmpty()) put("notifications", FieldValue.arrayUnion(*notes.toTypedArray()))
        }
        userDoc(uid).set(data, SetOptions.merge()).await()
    }

    /**
     * Records a finished lesson together with the Message-tab entries it earns: the first completion of
     * a lesson and the completion of the whole course. Returns true when this is the first lesson of the day
     * (the app then shows the "Clocking in!" card).
     */
    suspend fun recordLesson(uid: String, course: Course, lesson: Lesson, profile: UserProfile?): Boolean {
        val done = profile?.progress?.get(course.id).orEmpty()
        val firstToday = (profile?.minutesOn(LocalDate.now()) ?: 0) == 0
        val notes = buildList {
            if (lesson.id !in done) {
                add(
                    notification(
                        kind = AppNotification.KIND_NOTIFICATION,
                        type = "lesson",
                        title = "Congratulations on completing the lesson",
                        text = "«${lesson.title}» in ${course.title}",
                    )
                )
                if ((done + lesson.id).toSet().containsAll(course.lessons.map { it.id })) {
                    add(
                        notification(
                            kind = AppNotification.KIND_MESSAGE,
                            type = "course",
                            title = "Online Learning",
                            text = "Congratulations, you have completed «${course.title}»! Pick your next course on the Course tab.",
                        )
                    )
                }
            }
        }
        completeLesson(uid, course.id, lesson.id, lesson.durationMinutes, notes)
        return firstToday
    }

    private fun welcomeMessage() = notification(
        kind = AppNotification.KIND_MESSAGE,
        type = "welcome",
        title = "Online Learning",
        text = "Welcome! Start with a free course: History of Computing or Android with Kotlin. " +
            "Tap a lesson to listen; your progress is saved to your account.",
    )

    companion object {
        /** A Message-tab entry as stored in Firestore. */
        fun notification(kind: String, type: String, title: String, text: String): Map<String, Any> = mapOf(
            "kind" to kind,
            "type" to type,
            "title" to title,
            "text" to text,
            "time" to System.currentTimeMillis(),
        )
    }

    private fun userDoc(uid: String): DocumentReference =
        (db ?: throw FirebaseNotConfiguredException()).collection("users").document(uid)
}
