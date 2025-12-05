package com.cs407.myapplication.data


import com.cs407.myapplication.viewModels.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FirebaseProfileRepository {

    // Auth 和 Firestore 的单例
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    private fun currentUid(): String? = auth.currentUser?.uid

    fun loadProfile(
        onSuccess: (UserProfile?) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        val uid = currentUid()
        if (uid == null) {
            onError(IllegalStateException("User not logged in"))
            return
        }

        db.collection("userProfiles")
            .document(uid)
            .get()
            .addOnSuccessListener { snapshot ->
                val profile = snapshot.toObject(UserProfile::class.java)
                onSuccess(profile)
            }
            .addOnFailureListener { e ->
                onError(e)
            }
    }

    fun saveProfile(
        profile: UserProfile,
        onSuccess: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        val uid = currentUid()
        if (uid == null) {
            onError(IllegalStateException("User not logged in"))
            return
        }

        db.collection("userProfiles")
            .document(uid)
            .set(profile)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onError(e) }
    }
}
