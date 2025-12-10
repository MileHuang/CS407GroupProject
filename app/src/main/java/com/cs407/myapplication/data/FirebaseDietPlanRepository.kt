package com.cs407.myapplication.data

import com.cs407.myapplication.network.DietPlanResponseDto
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FirebaseDietPlanRepository {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    private fun currentUid(): String? = auth.currentUser?.uid


    fun loadLatestPlan(
        onSuccess: (DietPlanResponseDto?) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        val uid = currentUid()
        if (uid == null) {
            onError(IllegalStateException("User not logged in"))
            return
        }

        db.collection("userDietPlans")
            .document(uid)
            .get()
            .addOnSuccessListener { snapshot ->
                val plan = snapshot.toObject(DietPlanResponseDto::class.java)
                onSuccess(plan)
            }
            .addOnFailureListener { e ->
                onError(e)
            }
    }


    fun saveLatestPlan(
        plan: DietPlanResponseDto,
        onSuccess: () -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) {
        val uid = currentUid()
        if (uid == null) {
            onError(IllegalStateException("User not logged in"))
            return
        }

        db.collection("userDietPlans")
            .document(uid)
            .set(plan)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onError(e) }
    }
}
