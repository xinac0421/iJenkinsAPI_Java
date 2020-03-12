package com.hogwarts.tools;

public class Timer {
    public static void wait(int second){
        try {
            Thread.sleep(second * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
