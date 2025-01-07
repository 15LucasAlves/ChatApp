package com.example.chatapp.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.viewModelScope
import com.example.chatapp.R
import com.example.chatapp.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * This class extends the FirebaseMessagingService and is responsible for handling
 * Firebase Cloud Messaging (FCM) events, such as receiving new tokens and incoming messages.
 */
class FirebaseMessagingService : FirebaseMessagingService() {

    /**
     * This method is called when FCM provides a new token for the app.
     * This typically happens when the app is reinstalled or the user clears the app's data.
     * In this implementation, the new token is logged for debugging purposes.
     * You may want to update your Firestore or ViewModel with the new token.
     *
     * @param token The new token provided by FCM.
     */
    @OptIn(DelicateCoroutinesApi::class)
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("MyFirebaseMsgService", "Refreshed token: $token")
        val auth = FirebaseAuth.getInstance()
        auth.currentUser?.email?.let { email ->
            val userRepository = UserRepository()
            GlobalScope.launch(Dispatchers.IO) {
                userRepository.updateFcmToken(email, token)
            }
        }
    }

    /**
     * This method is called when a new message is received from FCM.
     * It checks if the message contains a notification payload and displays a notification
     * to the user. If the message contains a data payload, you can handle it as needed.
     *
     * @param remoteMessage The RemoteMessage object containing the message data.
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // If you are receiving a notification payload:
        remoteMessage.notification?.let {
            val title = it.title ?: "New message!"
            val body = it.body ?: "You received a new message."
            showNotification(title, body)
        }

        // If you are receiving data in 'remoteMessage.data', handle it as needed
        // val dataPayload = remoteMessage.data
    }

    /**
     * This private function is responsible for displaying a notification to the user.
     * It creates a notification channel (for Android 8.0 and above) and builds the notification
     * using the NotificationCompat.Builder. The notification is then displayed using the
     * NotificationManagerCompat.
     *
     * @param title The title of the notification.
     * @param body The body of the notification.
     */
    private fun showNotification(title: String, body: String) {
        val channelId = "chat_notifications"

        // Create the channel on Android 8.0+ (no-op on older versions)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            channelId,
            "Chat Notifications",
            NotificationManager.IMPORTANCE_HIGH
        )
        manager.createNotificationChannel(channel)

        // Build the notification
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        // Show the notification
        NotificationManagerCompat.from(this)
            .notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }
}