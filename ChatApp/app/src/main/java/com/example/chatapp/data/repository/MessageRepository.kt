package com.example.chatapp.data.repository

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

    // Group methods
    fun getGroup(groupId: String): Flow<Result<Group>> = callbackFlow {
        val subscription = groupsCollection.document(groupId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.Error(Exception(error.message)))
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
        } catch (e: Exception) {
            throw Exception("Failed to update group name: ${e.message}")
        }
    }

    // Message methods with read receipts
    fun getMessagesForChat(
        chatId: String,
        pageSize: Int,
        lastTimestamp: Long?
    ): Flow<Result<List<Message>>> = callbackFlow {
        var query = messagesCollection
            .whereEqualTo("chatId", chatId)
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
            messagesCollection.add(message.copy(readBy = listOf(message.senderEmail))).await()
        } catch (e: Exception) {
            throw Exception("Failed to send message: ${e.message}")
        }
    }

    suspend fun markMessageAsRead(messageId: String, readerEmail: String) {
        try {
            messagesCollection.document(messageId)
                .update("readBy", FieldValue.arrayUnion(readerEmail))
                .await()
        } catch (e: Exception) {
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
            throw Exception("Failed to mark messages as read: ${e.message}")
        }
    }
}