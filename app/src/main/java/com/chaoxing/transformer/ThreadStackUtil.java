package com.chaoxing.transformer;

import android.util.Log;

/**
 *
 */
public class ThreadStackUtil {
    public static void dumpCurrentThreadStackTrace(String tag) {
        try {
            StringBuilder builder = new StringBuilder();
            Thread currentThread = Thread.currentThread();
            StackTraceElement[] stackTraceElements = currentThread.getStackTrace();
            for (StackTraceElement element : stackTraceElements) {
                builder.append(element.toString()).append("\n");
            }
            Log.e(tag, builder.toString());
        } catch (Exception e) {
            Log.e(tag, "dumpCurrentThreadStackTrace err");
        }
    }
}
