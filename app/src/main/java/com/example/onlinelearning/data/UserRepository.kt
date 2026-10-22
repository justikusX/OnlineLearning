package com.example.onlinelearning.data

import com.example.onlinelearning.data.model.UserProfile
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await

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
        }
        doc.set(data, SetOptions.merge()).await()
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
    suspend fun purchase(uid: String, courseId: String) {
        userDoc(uid).set(mapOf("purchased" to FieldValue.arrayUnion(courseId)), SetOptions.merge()).await()
    }

    suspend fun completeLesson(uid: String, courseId: String, lessonId: String, minutes: Int) {
        userDoc(uid).set(
            mapOf(
                "progress" to mapOf(courseId to FieldValue.arrayUnion(lessonId)),
                "learnedTodayMinutes" to FieldValue.increment(minutes.toLong()),
            ),
            SetOptions.merge(),
        ).await()
    }

    private fun userDoc(uid: String): DocumentReference =
        (db ?: throw FirebaseNotConfiguredException()).collection("users").document(uid)
}
