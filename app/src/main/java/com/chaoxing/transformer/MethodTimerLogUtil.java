package com.chaoxing.transformer;

import android.util.Log;

/**
 *
 */
public class MethodTimerLogUtil {

    private static final String TAG = "MethodTimerLogUtil";

    public static void log(String method,long cost,String thread){
        Log.d(TAG,"source:"+method+" cost:"+cost+"ms "+ "thread:"+thread);
    }

//    public static void log(String method,long cost){
//        Log.d(TAG,"source:"+method+" cost:"+cost+"ms ");
//    }
}
