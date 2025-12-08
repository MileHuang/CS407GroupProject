package com.cs407.myapplication.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _profile = MutableStateFlow<UserProfile?>(null)
    val profile: StateFlow<UserProfile?> = _profile

    /** 读取 Cloud Firestore 的用户资料 */
    fun loadProfile() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("userProfiles")
            .document(uid)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val p = doc.toObject(UserProfile::class.java)
                    _profile.value = p
                }
            }
            .addOnFailureListener {
                // 直接 ignore，避免 UI 崩溃
            }
    }

    /** 保存用户资料到 Cloud Firestore */
    fun saveProfile(
        profile: UserProfile,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val uid = auth.currentUser?.uid ?: return

        db.collection("userProfiles")
            .document(uid)
            .set(profile)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onError(e) }
    }
    fun logout() {
        FirebaseAuth.getInstance().signOut()
        _profile.value = null
    }
}
