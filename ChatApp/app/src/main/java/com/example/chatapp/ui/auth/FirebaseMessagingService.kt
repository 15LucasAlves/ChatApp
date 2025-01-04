package com.example.chatapp.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.chatapp.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // This is called when FCM gives you a new token (e.g. app reinstall, user cleared data)
        // Typically you’d update Firestore here, or reuse your ViewModel approach
        Log.d("MyFirebaseMsgService", "Refreshed token: $token")
    }

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