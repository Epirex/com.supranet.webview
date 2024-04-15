package com.supranet.webview

import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val url = remoteMessage.data["url"]
        val intent = Intent("UPDATE_URL")
        intent.putExtra("newUrl", url)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }
}