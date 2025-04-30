package com.chaoxing.transformer;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.os.Build;

import com.fxa.transformer.annotation.MethodReplace;

import java.io.IOException;
import java.util.Locale;

/**
 * 方法替换
 */
public class MethodReplaceUtil{

    @MethodReplace(
        targetClass = "android/provider/Settings$Secure",
        targetMethod = "getString",
        targetParameterTypes = {ContentResolver.class,String.class},
        methodType = "static")
    public static String getString(ContentResolver resolver, String name) throws IOException {
        String id = "11111";
        return id;
    }

    @MethodReplace(
        targetClass = "android/app/NotificationManager",
        targetMethod = "createNotificationChannel",
        targetParameterTypes = {NotificationChannel.class},
        methodType = "virtual")
    public static void createNotificationChannel(NotificationManager manager, NotificationChannel channel) throws IOException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(channel);
        }
        throw new NullPointerException("111111");
    }



    @MethodReplace(
        targetClass = "com/example/asmdemo/TestUtils",
        targetMethod = "getFun",
//        targetParameterTypes = {String.class,String.class},
        methodType = "static")
    public static String fun(String s1, String s2) throws IOException {
        String id = s1+" "+ s2+"_replaced1";
        return id;
    }

    @MethodReplace(
        targetClass = "com/example/asmdemo/TestUtils",
        targetMethod = "getFun",
//        targetParameterTypes = {String.class,String.class,Long.class},
        methodType = "static")
    public static String fun(String s1, String s2,Long l1) throws IOException {
        String id = s1+" "+ s2+" l1:"+l1+" _replaced2";
        return id;
    }


    // New non-static method to replace String.toUpperCase()
    @MethodReplace(
        targetClass = "java.lang.String",
        targetMethod = "toUpperCase",
        targetParameterTypes = {},
        methodType = "virtual")
    public static String toUpperCase1(String original) {
        return original.toUpperCase(Locale.US) + "_replaced";
    }

    @MethodReplace(
        targetClass = "java.lang.String",
        targetMethod = "toUpperCase",
        targetParameterTypes = {Locale.class},
        methodType = "virtual")
    public static String toUpperCase2(String original,Locale local) {
        return original.toUpperCase(Locale.US) + "_replaced2";
    }


}
