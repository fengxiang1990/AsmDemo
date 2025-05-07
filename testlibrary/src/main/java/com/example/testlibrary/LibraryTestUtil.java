package com.example.testlibrary;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.Locale;

/**
 *
 */
public class LibraryTestUtil {

    public static void showNotification(Context context, String title, String content) {
        String channelId = "my_channel_id";
        String channelName = "MyChannel";

        // Android 8.0+ 需要通知渠道
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Channel description");
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(content)
            .setGroup("2222")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(context);
        notificationManagerCompat.notify(1, builder.build());  // 第一个参数是通知的唯一ID
    }

    public static void test(Context context){
        String id = Settings.Secure.getString(context.getContentResolver(),"android_id");
        Log.e("fxa","third library android id->"+id);
        String newId = id.toUpperCase();
        Log.e("fxa","third library newId id->"+newId);
        String newId2 = newId.toUpperCase(Locale.CANADA);
        Log.e("fxa","third library newId2 id2->"+newId2);
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        Log.d("LibraryTestUtil","LibraryTestUtil test");
    }
}
