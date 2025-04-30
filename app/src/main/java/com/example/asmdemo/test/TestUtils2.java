package com.example.asmdemo.test;

import android.util.Log;

/**
 *
 */
public class TestUtils2 {

    public void test1(){
        try {
            Thread.sleep(3000);
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
