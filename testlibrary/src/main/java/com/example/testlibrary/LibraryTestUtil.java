package com.example.testlibrary;

import android.util.Log;

/**
 *
 */
public class LibraryTestUtil {

    public static void test(){
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        Log.d("LibraryTestUtil","LibraryTestUtil test");
    }
}
