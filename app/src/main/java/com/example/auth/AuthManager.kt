package com.example.auth

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UserProfile(
    val uid: String,
    val displayName: String,
    val email: String,
    val photoUrl: String = "",
    val isDemo: Boolean = false
)

object AuthManager {
    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    private var firebaseAuth: FirebaseAuth? = null

    init {
        try {
            firebaseAuth = FirebaseAuth.getInstance()
            val fbUser = firebaseAuth?.currentUser
            if (fbUser != null) {
                _currentUser.value = UserProfile(
                    uid = fbUser.uid,
                    displayName = fbUser.displayName ?: "QR Pro User",
                    email = fbUser.email ?: "user@qrstudio.pro",
                    photoUrl = fbUser.photoUrl?.toString() ?: ""
                )
            }
        } catch (e: Exception) {
            Log.e("AuthManager", "Firebase init not available: ${e.message}")
        }
    }

    fun signInDemoUser(name: String = "Alex Rivera", email: String = "alex.rivera@qrstudio.dev") {
        _currentUser.value = UserProfile(
            uid = "demo_user_" + System.currentTimeMillis(),
            displayName = name,
            email = email,
            photoUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150",
            isDemo = true
        )
    }

    fun signOut() {
        try {
            firebaseAuth?.signOut()
        } catch (e: Exception) {
            Log.e("AuthManager", "Sign out error: ${e.message}")
        }
        _currentUser.value = null
    }

    fun updateFromFirebase(fbUser: FirebaseUser) {
        _currentUser.value = UserProfile(
            uid = fbUser.uid,
            displayName = fbUser.displayName ?: "Firebase User",
            email = fbUser.email ?: "user@gmail.com",
            photoUrl = fbUser.photoUrl?.toString() ?: ""
        )
    }
}
