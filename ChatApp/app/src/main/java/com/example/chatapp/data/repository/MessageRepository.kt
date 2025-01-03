package com.example.chatapp.data.repository

import android.util.Log
import com.example.chatapp.data.model.Message
import com.example.chatapp.data.model.Group
import com.example.chatapp.data.Result
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

    /**
     * Retrieves a group by its ID.
     */
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

    /**
     * Updates the name of a group.
     */
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

    /**
     * Retrieves messages for an individual chat, ensuring messages are unique to the chat.
     */
    fun getMessagesForChat(
        chatId: String,
        pageSize: Int,
        lastTimestamp: Long?
    ): Flow<Result<List<Message>>> = callbackFlow {
        var query = messagesCollection
            .whereEqualTo("chatId", chatId)
            .whereEqualTo("groupMessage", false) // Ensure only individual chat messages are fetched
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

            Log.d(TAG, "Fetched ${messages.size} messages for chatId '$chatId'")
            trySend(Result.Success(messages))
        }

        awaitClose { subscription.remove() }
    }

    /**
     * Retrieves last message so it can be displayed in the userselectionscreen.
     */
    fun fetchLastMessage(
        chatId: String,
    ): Flow<Result<List<Message>>> = callbackFlow {
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

            Log.d(TAG, "Fetched ${messages.size} messages for chatId '$chatId'")
            trySend(Result.Success(messages))
        }

        awaitClose { subscription.remove() }
    }

    /**
     * Retrieves messages for a group chat, ensuring messages are unique to the group.
     */
    fun getGroupMessages(
        groupId: String,
        pageSize: Int,
        lastTimestamp: Long?
    ): Flow<Result<List<Message>>> = callbackFlow {
        var query = messagesCollection
            .whereEqualTo("groupId", groupId)
            .whereEqualTo("groupMessage", true) // Ensure only group chat messages are fetched
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(pageSize.toLong())

        lastTimestamp?.let { timestamp ->
            query = query.startAfter(timestamp)
        }

        val subscription = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(Result.Error(Exception(error.message)))
                Log.e(TAG, "Error fetching group messages for groupId '$groupId': ${error.message}")
                return@addSnapshotListener
            }

            val messages = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(Message::class.java)?.copy(id = doc.id)
            } ?: emptyList()

            Log.d(TAG, "Fetched ${messages.size} group messages for groupId '$groupId'")
            trySend(Result.Success(messages))
        }

        awaitClose { subscription.remove() }
    }

    /**
     * Sends a message to Firestore.
     * For individual chats, initializes the `readBy` field with the recipient's email.
     */
    suspend fun sendMessage(message: Message) {
        try {
            // Initialize readBy for individual chats
            val messageToSend = if (!message.groupMessage) {
                message.copy(readBy = listOf(message.recipientEmail))
            } else {
                message
            }
            messagesCollection.add(messageToSend).await()
            Log.d(TAG, "Message sent with readBy: ${messageToSend.readBy}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send message: ${e.message}")
            throw Exception("Failed to send message: ${e.message}")
        }
    }

    /**
     * Marks a single message as read by adding the reader's email to the `readBy` field.
     */
    suspend fun markMessageAsRead(messageId: String, readerEmail: String) {
        try {
            messagesCollection.document(messageId)
                .update("readBy", FieldValue.arrayUnion(readerEmail))
                .await()
            Log.d(TAG, "Marked message '$messageId' as read by '$readerEmail'")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to mark message as read: ${e.message}")
            throw Exception("Failed to mark message as read: ${e.message}")
        }
    }

    /**
     * Marks multiple messages as read in a batch operation.
     */
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
            Log.d(TAG, "Batch marked ${messages.size} messages as read by '$readerEmail'")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to mark messages as read: ${e.message}")
            throw Exception("Failed to mark messages as read: ${e.message}")
        }
    }

    /**
     * Edits the text of a message and updates the `edited` and `editedAt` fields.
     */
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
            Log.d(TAG, "Edited message '$messageId' with new text: '$newText'")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to edit message: ${e.message}")
            throw Exception("Failed to edit message: ${e.message}")
        }
    }

    /**
     * Deletes a message from Firestore.
     */
    suspend fun deleteMessage(messageId: String) {
        try {
            messagesCollection.document(messageId)
                .delete()
                .await()
            Log.d(TAG, "Deleted message '$messageId'")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete message: ${e.message}")
            throw Exception("Failed to delete message: ${e.message}")
        }
    }

    // --------------------- Group Member Management ---------------------

    /**
     * Adds a member to a group.
     */
    suspend fun addMemberToGroup(groupId: String, email: String) {
        try {
            val groupRef = groupsCollection.document(groupId)
            groupRef.update("members", FieldValue.arrayUnion(email)).await()
            Log.d(TAG, "Added member '$email' to group '$groupId'")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add member to group: ${e.message}")
            throw Exception("Failed to add member to group: ${e.message}")
        }
    }

    /**
     * Removes a member from a group.
     */
    suspend fun removeMemberFromGroup(groupId: String, email: String) {
        try {
            val groupRef = groupsCollection.document(groupId)
            groupRef.update("members", FieldValue.arrayRemove(email)).await()
            Log.d(TAG, "Removed member '$email' from group '$groupId'")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove member from group: ${e.message}")
            throw Exception("Failed to remove member from group: ${e.message}")
        }
    }
}