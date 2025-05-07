package com.example.asmdemo;

import android.content.Context;
import android.provider.Settings;
import android.util.Log;

import com.example.testlibrary.LibraryTestUtil;

import java.util.Locale;

/**
 *
 */
public class TestUtils {

    public static String getFun(String s1,String s2){
        return "fxa"+s1+s2;
    }

    public static String getFun(String s1,String s2,Long l3){
        return "fxa2"+s1+s2+l3;
    }

    public void test1(Context context){
        try {
            Thread.sleep(3000);
            String id = Settings.Secure.getString(context.getContentResolver(),"android_id");
            Log.e("fxa","android id->"+id);
            String newId = id.toUpperCase();
            Log.e("fxa","newId id->"+newId);
            String newId2 = newId.toUpperCase(Locale.CANADA);
            Log.e("fxa","newId id2->"+newId2);
            String s1 = TestUtils.getFun("hello","world");
            String s2 = TestUtils.getFun("hello","world",10000L);
            Log.e("fxa","s1->"+s1);
            Log.e("fxa","s2->"+s2);

            LibraryTestUtil.showNotification(context,"hahaha","收到一条消息");
        } catch (InterruptedException e) {

        }
    }

    public String test2(){
        try {
            Thread.sleep(1000);
            new Thread(){
                @Override
                public void run() {
                    test3();
                }
            }.start();
        } catch (InterruptedException e) {

        }
        return "sss";
    }

    public void test3(){
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {

        }
    }
}
