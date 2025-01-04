package com.example.chatapp.data.repository

import android.util.Log
import com.example.chatapp.data.Result
import com.example.chatapp.data.model.Group
import com.example.chatapp.data.model.Message
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class MessageRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val messagesCollection = firestore.collection("messages")
    private val groupsCollection = firestore.collection("groups")
    private val TAG = "MessageRepository"

    // --------------------- Group Methods ---------------------
    fun getGroup(groupId: String): Flow<Result<Group>> = callbackFlow {
        val subscription = groupsCollection.document(groupId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.Error(Exception(error.message)))
                    Log.e(TAG, "Error fetching group: ${error.message}")
                    return@addSnapshotListener
                }

                val group = snapshot?.toObject(Group::class.java)?.copy(id = snapshot.id)
                if (group != null) {
                    trySend(Result.Success(group))
                } else {
                    trySend(Result.Error(Exception("Group not found")))
                }
            }

        awaitClose { subscription.remove() }
    }

    suspend fun updateGroupName(groupId: String, newName: String) {
        try {
            groupsCollection.document(groupId)
                .update("name", newName)
                .await()
            Log.d(TAG, "Updated group name to '$newName' for groupId '$groupId'")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update group name: ${e.message}")
            throw Exception("Failed to update group name: ${e.message}")
        }
    }

    // --------------------- Message Methods ---------------------
    fun getMessagesForChat(
        chatId: String,
        pageSize: Int,
        lastTimestamp: Long?
    ): Flow<Result<List<Message>>> = callbackFlow {
        var query = messagesCollection
            .whereEqualTo("chatId", chatId)
            .whereEqualTo("groupMessage", false)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(pageSize.toLong())

        lastTimestamp?.let { timestamp ->
            query = query.startAfter(timestamp)
        }

        val subscription = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(Result.Error(Exception(error.message)))
                Log.e(TAG, "Error fetching messages for chatId '$chatId': ${error.message}")
                return@addSnapshotListener
            }

            val messages = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(Message::class.java)?.copy(id = doc.id)
            } ?: emptyList()

            trySend(Result.Success(messages))
        }

        awaitClose { subscription.remove() }
    }

    fun fetchLastMessage(chatId: String): Flow<Result<List<Message>>> = callbackFlow {
        val query = messagesCollection
            .whereEqualTo("chatId", chatId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(1)

        val subscription = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(Result.Error(Exception(error.message)))
                Log.e(TAG, "Error fetching messages for chatId '$chatId': ${error.message}")
                return@addSnapshotListener
            }

            val messages = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(Message::class.java)?.copy(id = doc.id)
            } ?: emptyList()

            trySend(Result.Success(messages))
        }

        awaitClose { subscription.remove() }
    }

    fun getGroupMessages(
        groupId: String,
        pageSize: Int,
        lastTimestamp: Long?
    ): Flow<Result<List<Message>>> = callbackFlow {
        var query = messagesCollection
            .whereEqualTo("groupId", groupId)
            .whereEqualTo("groupMessage", true)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(pageSize.toLong())

        lastTimestamp?.let { timestamp ->
            query = query.startAfter(timestamp)
        }

        val subscription = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(Result.Error(Exception(error.message)))
                return@addSnapshotListener
            }

            val messages = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(Message::class.java)?.copy(id = doc.id)
            } ?: emptyList()

            trySend(Result.Success(messages))
        }

        awaitClose { subscription.remove() }
    }

    suspend fun sendMessage(message: Message) {
        try {
            // Initialize readBy for individual chats
            val messageToSend = if (!message.groupMessage) {
                message.copy(readBy = listOf(message.recipientEmail))
            } else {
                message
            }
            messagesCollection.add(messageToSend).await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send message: ${e.message}")
            throw Exception("Failed to send message: ${e.message}")
        }
    }

    suspend fun markMessageAsRead(messageId: String, readerEmail: String) {
        try {
            messagesCollection.document(messageId)
                .update("readBy", FieldValue.arrayUnion(readerEmail))
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to mark message as read: ${e.message}")
            throw Exception("Failed to mark message as read: ${e.message}")
        }
    }

    suspend fun markMessagesAsRead(messages: List<Message>, readerEmail: String) {
        try {
            val batch = firestore.batch()
            messages.forEach { message ->
                if (!message.readBy.contains(readerEmail)) {
                    batch.update(
                        messagesCollection.document(message.id),
                        "readBy",
                        FieldValue.arrayUnion(readerEmail)
                    )
                }
            }
            batch.commit().await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to mark messages as read: ${e.message}")
            throw Exception("Failed to mark messages as read: ${e.message}")
        }
    }

    suspend fun editMessage(messageId: String, newText: String) {
        try {
            messagesCollection.document(messageId)
                .update(
                    mapOf(
                        "text" to newText,
                        "edited" to true,
                        "editedAt" to System.currentTimeMillis()
                    )
                )
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to edit message: ${e.message}")
            throw Exception("Failed to edit message: ${e.message}")
        }
    }

    suspend fun deleteMessage(messageId: String) {
        try {
            messagesCollection.document(messageId)
                .delete()
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete message: ${e.message}")
            throw Exception("Failed to delete message: ${e.message}")
        }
    }

    // --------------------- Group Member Management ---------------------
    suspend fun addMemberToGroup(groupId: String, email: String) {
        try {
            val groupRef = groupsCollection.document(groupId)
            groupRef.update("members", FieldValue.arrayUnion(email)).await()
        } catch (e: Exception) {
            throw Exception("Failed to add member to group: ${e.message}")
        }
    }

    suspend fun removeMemberFromGroup(groupId: String, email: String) {
        try {
            val groupRef = groupsCollection.document(groupId)
            groupRef.update("members", FieldValue.arrayRemove(email)).await()
        } catch (e: Exception) {
            throw Exception("Failed to remove member from group: ${e.message}")
        }
    }
}