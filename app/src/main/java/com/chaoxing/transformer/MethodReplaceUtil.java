package com.chaoxing.transformer;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;


import java.io.IOException;
import java.util.Locale;

/**
 * 方法替换
 */
public class MethodReplaceUtil{


    public static String getString(ContentResolver resolver, String name) throws IOException {
        String id = "11111";
        return id;
    }


    public static void createNotificationChannel(NotificationManager manager, NotificationChannel channel) throws IOException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(channel);
        }
        ThreadStackUtil.dumpCurrentThreadStackTrace("createNotificationChannel");
    }



    public static NotificationCompat.Builder setGroup(NotificationCompat.Builder builder,String group) throws IOException {
        ThreadStackUtil.dumpCurrentThreadStackTrace("setGroup");
        builder.setGroup(group);
        Log.e("fxa","set group->"+group);
        return builder;
    }


    public static String fun(String s1, String s2) throws IOException {
        String id = s1+" "+ s2+"_replaced1";
        return id;
    }


    public static String fun(String s1, String s2,Long l1) throws IOException {
        String id = s1+" "+ s2+" l1:"+l1+" _replaced2";
        return id;
    }


    public static String toUpperCase1(String original) {
        return original.toUpperCase(Locale.US) + "_replaced";
    }


    public static String toUpperCase2(String original,Locale local) {
        return original.toUpperCase(Locale.US) + "_replaced2";
    }


}
