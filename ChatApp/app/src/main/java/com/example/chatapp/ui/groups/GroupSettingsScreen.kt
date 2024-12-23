package com.example.chatapp.ui.groups

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.chatapp.data.model.User

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupSettingsScreen(
    groupId: String,
    currentUserEmail: String,
    onNavigateBack: () -> Unit,
    viewModel: GroupSettingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    var showAddMember by remember { mutableStateOf(false) }
    var showEditName by remember { mutableStateOf(false) }
    val group by viewModel.group.collectAsState()
    val members by viewModel.members.collectAsState()
    val error by viewModel.error.collectAsState()

    LaunchedEffect(groupId) {
        viewModel.loadGroup(groupId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Group Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Group Image and Name
            item {
                GroupHeader(
                    group = group,
                    isAdmin = group?.createdBy == currentUserEmail,
                    onEditImage = { /* Handle image edit */ },
                    onEditName = { showEditName = true }
                )
            }

            // Members Section
            item {
                Text(
                    text = "Members (${members.size})",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            // Members List
            items(members) { member ->
                MemberItem(
                    user = member,
                    isAdmin = group?.createdBy == currentUserEmail,
                    onRemove = {
                        if (member.email != currentUserEmail) {
                            viewModel.removeMember(groupId, member.email)
                        }
                    }
                )
            }

            // Add Member Button (only for admin)
            if (group?.createdBy == currentUserEmail) {
                item {
                    Button(
                        onClick = { showAddMember = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Member")
                    }
                }
            }

            // Leave Group Button
            item {
                Button(
                    onClick = {
                        viewModel.leaveGroup(groupId, currentUserEmail)
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.ExitToApp, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Leave Group")
                }
            }
        }

        if (error.isNotEmpty()) {
            Snackbar(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(error)
            }
        }
    }

    if (showAddMember) {
        AddMemberDialog(
            onDismiss = { showAddMember = false },
            onAddMember = { email ->
                viewModel.addMember(groupId, email)
                showAddMember = false
            }
        )
    }

    if (showEditName) {
        EditGroupNameDialog(
            currentName = group?.name ?: "",
            onDismiss = { showEditName = false },
            onConfirm = { newName ->
                viewModel.updateGroupName(groupId, newName)
                showEditName = false
            }
        )
    }
}

@Composable
private fun GroupHeader(
    group: Group?,
    isAdmin: Boolean,
    onEditImage: () -> Unit,
    onEditName: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(group?.photoUrl ?: "https://via.placeholder.com/100")
                    .crossfade(true)
                    .build(),
                contentDescription = "Group photo",
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            if (isAdmin) {
                IconButton(
                    onClick = onEditImage,
                    modifier = Modifier.align(Alignment.BottomEnd)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit photo",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = group?.name ?: "",
                style = MaterialTheme.typography.headlineSmall
            )
            if (isAdmin) {
                IconButton(onClick = onEditName) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit name")
                }
            }
        }
    }
}

@Composable
private fun MemberItem(
    user: User,
    isAdmin: Boolean,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(user.photoUrl ?: "https://via.placeholder.com/40")
                .crossfade(true)
                .build(),
            contentDescription = "Profile photo",
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.username ?: user.email,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = user.email,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (isAdmin) {
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Remove, contentDescription = "Remove member")
            }
        }
    }
} 