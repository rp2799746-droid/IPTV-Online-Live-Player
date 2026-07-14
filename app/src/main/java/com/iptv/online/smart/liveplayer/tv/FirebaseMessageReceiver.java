package com.iptv.online.smart.liveplayer.tv;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.iptv.online.smart.liveplayer.tv.Activity.MainActivity;

public class FirebaseMessageReceiver extends FirebaseMessagingService {

    private static final String TAG = "FirebaseMessageReceiver";
    private static final String CHANNEL_ID = "default_channel";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        // Try-catch: notification build/post ma koi exception aave to e propagate na thay
        // (FCM delivery fail -> system nu CannotDeliverBroadcastException aatakva madad kare).
        try {
            if (remoteMessage.getData().size() > 0) {
                String link = remoteMessage.getData().get("link");

                if (link != null && !link.isEmpty()) {
                    showNotification(link);
                } else {
                    Log.e("FCM", "No link found in the notification data");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "onMessageReceived error", e);
        }
    }

    private void showNotification(String link) {
      try {
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (notificationManager == null) return;

        // Android 13+ : POST_NOTIFICATIONS permission na hoy to notify skip karo
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "POST_NOTIFICATIONS permission nathi -> notification skip");
                return;
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Default Channel",
                    NotificationManager.IMPORTANCE_HIGH
            );
            notificationManager.createNotificationChannel(channel);
        }

        Intent appIntent = new Intent(this, MainActivity.class);
        appIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        appIntent.putExtra("link", link);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                appIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("New Notification")
                .setContentText("Tap to open the link")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);


        notificationManager.notify(1, notificationBuilder.build());
      } catch (Exception e) {
        Log.e(TAG, "showNotification error", e);
      }
    }
}