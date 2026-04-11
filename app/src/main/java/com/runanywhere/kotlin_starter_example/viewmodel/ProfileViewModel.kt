package com.runanywhere.kotlin_starter_example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.runanywhere.kotlin_starter_example.data.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class ProfileState {
    object Idle : ProfileState()
    object Loading : ProfileState()
    object Success : ProfileState()
    data class Error(val message: String) : ProfileState()
}

class ProfileViewModel : ViewModel() {
    private val auth = Firebase.auth
    private val db = Firebase.firestore
    
    private val _profileState = MutableStateFlow<ProfileState>(ProfileState.Idle)
    val profileState: StateFlow<ProfileState> = _profileState

    fun loadProfile() {
        viewModelScope.launch {
            val user = auth.currentUser
            if (user == null) {
                Log.e("ProfileViewModel", "Load failed: No user authenticated")
                return@launch
            }
            
            _profileState.value = ProfileState.Loading
            try {
                // Refresh token to ensure we have valid permissions
                user.getIdToken(true).await()
                
                val snapshot = db.collection("users").document(user.uid).get().await()
                if (snapshot.exists()) {
                    val name = snapshot.getString("name") ?: ""
                    val phone = snapshot.getString("phone") ?: ""
                    val emergency = snapshot.getString("emergencyContact") ?: ""
                    
                    // Update local repository
                    SettingsRepository.setUserName(name)
                    SettingsRepository.setUserPhone(phone)
                    SettingsRepository.setEmergencyContact(emergency)
                }
                _profileState.value = ProfileState.Success
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error loading profile for UID: ${user.uid}", e)
                _profileState.value = ProfileState.Error(e.localizedMessage ?: "Failed to load profile")
            }
        }
    }

    fun saveProfile(name: String, phone: String, emergency: String) {
        viewModelScope.launch {
            val user = auth.currentUser
            if (user == null) {
                _profileState.value = ProfileState.Error("Session expired. Please log in again.")
                return@launch
            }
            
            _profileState.value = ProfileState.Loading
            try {
                // Force refresh token to prove identity to Firestore
                user.getIdToken(true).await()
                
                val profileData = hashMapOf(
                    "name" to name,
                    "email" to (user.email ?: ""),
                    "phone" to phone,
                    "emergencyContact" to emergency,
                    "updatedAt" to System.currentTimeMillis()
                )
                
                Log.d("ProfileViewModel", "Saving to path: users/${user.uid}")
                
                // Matches your security rule: match /users/{userId}
                db.collection("users").document(user.uid).set(profileData).await()
                
                // Update local repository
                SettingsRepository.setUserName(name)
                SettingsRepository.setUserPhone(phone)
                SettingsRepository.setEmergencyContact(emergency)
                
                _profileState.value = ProfileState.Success
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Permission error saving for UID: ${user.uid}", e)
                _profileState.value = ProfileState.Error("Access Denied: Please check Firebase rules or UID match.")
            }
        }
    }
    
    fun clearState() {
        _profileState.value = ProfileState.Idle
    }
}
