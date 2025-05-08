package com.fxa.testlib;

public class MyClass {
    public int compareTo(int a,int b){
        Integer i1 = a;
        Integer i2 = b;
        return i1.compareTo(i2);
    }

    public boolean equals(int a,int b){
        String a1 = a+"";
        String b1 = b+"";
        String s1 = a1.concat(b1);
        return s1.equals("hello world");
    }

    public String concat(String s1,String s2){
        return s1.concat(s2);
    }
}