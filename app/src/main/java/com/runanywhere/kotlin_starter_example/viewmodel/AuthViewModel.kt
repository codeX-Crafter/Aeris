package com.runanywhere.kotlin_starter_example.viewmodel

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val user: FirebaseUser?) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel : ViewModel() {
    private val auth: FirebaseAuth = Firebase.auth
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    val currentUser: FirebaseUser? get() = auth.currentUser

    init {
        // Check if user is already logged in
        if (auth.currentUser != null) {
            _authState.value = AuthState.Success(auth.currentUser)
        }
    }

    fun login(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _authState.value = AuthState.Error("Fields cannot be empty")
            return
        }
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val result = auth.signInWithEmailAndPassword(email, pass).await()
                _authState.value = AuthState.Success(result.user)
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.localizedMessage ?: "Login failed")
            }
        }
    }

    fun signUp(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _authState.value = AuthState.Error("Fields cannot be empty")
            return
        }
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val result = auth.createUserWithEmailAndPassword(email, pass).await()
                _authState.value = AuthState.Success(result.user)
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.localizedMessage ?: "Signup failed")
            }
        }
    }

    fun signInWithGoogle(context: Context) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val credentialManager = CredentialManager.create(context)
            
            val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId("640795411475-kdfbli3vnpnecfm0ir60851bdo2ntvt2.apps.googleusercontent.com")
                .setAutoSelectEnabled(false)
                .build()

            val request: GetCredentialRequest = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            try {
                val result = credentialManager.getCredential(context, request)
                handleGoogleSignIn(result)
            } catch (e: GetCredentialException) {
                Log.e("AuthViewModel", "GetCredentialException", e)
                _authState.value = AuthState.Error(e.localizedMessage ?: "Google Sign-In failed")
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Google Sign-In Exception", e)
                _authState.value = AuthState.Error(e.localizedMessage ?: "An error occurred")
            }
        }
    }

    private suspend fun handleGoogleSignIn(result: GetCredentialResponse) {
        val credential = result.credential
        Log.d("AuthViewModel", "Credential type returned: ${credential.type}")

        if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            try {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val googleIdToken = googleIdTokenCredential.idToken
                
                val firebaseCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                val authResult = auth.signInWithCredential(firebaseCredential).await()
                _authState.value = AuthState.Success(authResult.user)
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Firebase Auth with Google failed", e)
                _authState.value = AuthState.Error(e.localizedMessage ?: "Firebase authentication failed")
            }
        } else {
            Log.e("AuthViewModel", "Unexpected credential type: ${credential.type}")
            _authState.value = AuthState.Error("Unexpected account type selected")
        }
    }

    fun logout(context: Context) {
        viewModelScope.launch {
            auth.signOut()
            val credentialManager = CredentialManager.create(context)
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
            _authState.value = AuthState.Idle
        }
    }
}
