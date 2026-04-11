package com.runanywhere.kotlin_starter_example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.runanywhere.kotlin_starter_example.data.SettingsRepository
import com.runanywhere.kotlin_starter_example.viewmodel.ProfileState
import com.runanywhere.kotlin_starter_example.viewmodel.ProfileViewModel

@Composable
fun ProfileScreen(profileViewModel: ProfileViewModel) {
    val auth = Firebase.auth
    val currentUser = auth.currentUser
    val context = LocalContext.current
    
    val userName by SettingsRepository.userName.collectAsState()
    val userPhone by SettingsRepository.userPhone.collectAsState()
    val emergencyContact by SettingsRepository.emergencyContact.collectAsState()
    val profileState by profileViewModel.profileState.collectAsState()

    var nameInput by remember { mutableStateOf("") }
    var phoneInput by remember { mutableStateOf("") }
    var emergencyInput by remember { mutableStateOf("") }

    // Load profile from Firestore on start
    LaunchedEffect(Unit) {
        profileViewModel.loadProfile()
    }

    // Sync input with state when first loaded or changed externally
    LaunchedEffect(userName, userPhone, emergencyContact, currentUser) {
        if (userName.isBlank()) {
            val firebaseName = currentUser?.displayName ?: currentUser?.email?.substringBefore("@") ?: ""
            nameInput = firebaseName.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        } else {
            nameInput = userName
        }
        phoneInput = userPhone
        emergencyInput = emergencyContact
    }

    // Handle Profile States (Success/Error)
    LaunchedEffect(profileState) {
        when (profileState) {
            is ProfileState.Success -> {
                Toast.makeText(context, "Profile updated", Toast.LENGTH_SHORT).show()
                profileViewModel.clearState()
            }
            is ProfileState.Error -> {
                Toast.makeText(context, (profileState as ProfileState.Error).message, Toast.LENGTH_LONG).show()
                profileViewModel.clearState()
            }
            else -> {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(Color(0xFF6FB1FC), Color(0xFFA7C6FF))))
                .padding(horizontal = 24.dp, vertical = 40.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(60.dp),
                        tint = Color.White
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = if (nameInput.isBlank()) "Your Profile" else nameInput,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                if (currentUser?.email != null) {
                    Text(
                        text = currentUser.email ?: "",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Column(modifier = Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            ProfileInputField(
                label = "Full Name",
                value = nameInput,
                onValueChange = { nameInput = it },
                icon = Icons.Default.Badge,
                enabled = profileState !is ProfileState.Loading
            )

            ProfileInputField(
                label = "Phone Number",
                value = phoneInput,
                onValueChange = { phoneInput = it },
                icon = Icons.Default.Phone,
                enabled = profileState !is ProfileState.Loading
            )

            ProfileInputField(
                label = "Emergency Contact",
                value = emergencyInput,
                onValueChange = { emergencyInput = it },
                icon = Icons.Default.ContactPhone,
                enabled = profileState !is ProfileState.Loading
            )

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = {
                    profileViewModel.saveProfile(nameInput, phoneInput, emergencyInput)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6FB1FC)),
                enabled = profileState !is ProfileState.Loading
            ) {
                if (profileState is ProfileState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Save Profile", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            
            Text(
                "Your details are securely stored in the cloud and synced across devices.",
                fontSize = 12.sp,
                color = Color(0xFFB0B0B0),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun ProfileInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    icon: ImageVector,
    enabled: Boolean = true
) {
    Column {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF6B7A9A),
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            enabled = enabled,
            leadingIcon = { Icon(icon, null, tint = Color(0xFF6FB1FC)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black,
                focusedBorderColor = Color(0xFF6FB1FC),
                unfocusedBorderColor = Color(0xFFEEF0F5),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            )
        )
    }
}
