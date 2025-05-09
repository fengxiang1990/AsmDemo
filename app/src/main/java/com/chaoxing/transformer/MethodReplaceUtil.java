package com.chaoxing.transformer;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.app.NotificationCompat;


import java.io.IOException;
import java.util.Locale;

/**
 * 方法替换
 */
public class MethodReplaceUtil{

    public static void setOnClickListener(ImageView view, View.OnClickListener listener){
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ThreadStackUtil.dumpCurrentThreadStackTrace("fxa MethodReplaceUtil ImageView.onClick");
                listener.onClick(view);
            }
        });
    }

    public static void setOnClickListener(Button view, View.OnClickListener listener){
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ThreadStackUtil.dumpCurrentThreadStackTrace("fxa MethodReplaceUtil Button.onClick");
                listener.onClick(view);
            }
        });
    }

    public static void setOnClickListener(TextView view, View.OnClickListener listener){
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ThreadStackUtil.dumpCurrentThreadStackTrace("fxa MethodReplaceUtil TextView.onClick");
                listener.onClick(view);
            }
        });
    }

    public static void setOnClickListener(View view,View.OnClickListener listener){
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ThreadStackUtil.dumpCurrentThreadStackTrace("fxa MethodReplaceUtil View.onClick");
                listener.onClick(view);
            }
        });
    }

    public static int compareTo(Integer a,Integer b){
        Log.e("fxa","MethodReplaceUtil compareTo:"+a+" "+b);
        return a.compareTo(b) * 1 * 100;
    }

    public static String concat(String a,String b){
        Log.e("fxa","MethodReplaceUtil concat:"+a+" "+b);
        return a.concat(b).concat("_replaced");
    }

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
