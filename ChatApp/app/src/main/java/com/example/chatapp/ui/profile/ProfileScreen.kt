package com.example.chatapp.ui.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.chatapp.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    email: String,
    isInitialSetup: Boolean = false,
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit = {},
    viewModel: ProfileViewModel = viewModel(
        key = if (isInitialSetup) "initialSetup" else "profile"
    )
) {
    var isEditing by remember { mutableStateOf(isInitialSetup) }
    var username by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    val userState by viewModel.userState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    LaunchedEffect(Unit) {
        viewModel.loadUserProfile(email)
    }

    LaunchedEffect(userState) {
        userState?.let {
            username = it.username ?: ""
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (isInitialSetup) "Complete Your Profile" else "Profile") },
                navigationIcon = {
                    if (!isInitialSetup) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    if (!isInitialSetup) {
                        Row {
                            IconButton(onClick = { isEditing = !isEditing }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit profile")
                            }
                            IconButton(onClick = onLogout) {
                                Icon(Icons.Default.Logout, contentDescription = "Logout")
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile Image
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .clickable(enabled = isEditing) { 
                        if (isEditing) {
                            imagePicker.launch("image/*")
                        }
                    }
            ) {
                if (selectedImageUri != null || userState?.photoUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(selectedImageUri ?: userState?.photoUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Profile picture",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    val defaultProfileUrl =
                        "https://firebasestorage.googleapis.com/v0/b/chatapp-e94d4.firebasestorage.app/o/profile_images%2Fdefault_user_profile_image.png?alt=media&token=f19d8620-dacc-438b-aa53-b47199d89640"
                    // make sure the ?alt=media param is included so it returns the actual image
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(defaultProfileUrl)   // <-- fallback to your remote default
                            .crossfade(true)
                            .build(),
                        contentDescription = "Default profile picture",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            if (isEditing) {
                Text(
                    text = "Tap to change profile picture",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Email (non-editable)
            Text(
                text = "Email",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = email,
                style = MaterialTheme.typography.bodyLarge
            )

            // Username
            Text(
                text = "Username",
                style = MaterialTheme.typography.titleMedium
            )
            if (isEditing) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            } else {
                Text(
                    text = userState?.username ?: "No username set",
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            if (error.isNotEmpty()) {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (isEditing || isInitialSetup) {
                Button(
                    onClick = {
                        viewModel.updateProfile(
                            email = email,
                            username = username,
                            profileImageUri = selectedImageUri
                        ) {
                            if (isInitialSetup) {
                                onNavigateBack()
                            } else {
                                isEditing = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = username.isNotEmpty() && !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(if (isInitialSetup) "Complete Setup" else "Save Changes")
                    }
                }
            }
        }
    }
}