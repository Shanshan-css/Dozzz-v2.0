package com.example.speecktotext2.D;

import android.util.Log;

public class ShareData {
    private static String input;
    private static boolean hasInput = false;
    private static boolean isSpeakingOrNot = false;
    private static String userID;
    private static String password;
    private static String currentTime;
    private static long startTime;
    private static long endTime;
    private static long startTimeRob;
    private static long endTimeRob;

    private static final String TAG = ShareData.class.getSimpleName();

    private ShareData() {
    }
    public static void setInput(String value) {
        input = value;
        hasInput = true;
        //lock.notifyAll();
        Log.d(TAG, "ShareData的setInput值:"+input);
    }

    public static void setUsername(String value){
        userID = value;
    }

    public static String getUsername(){
        return userID;
    }

    public static void setPass(String value){
        password = value;
        Log.d(TAG, "获得到的密码为：" + value);
    }

    public static String getPass(){
        return password;
    }

    public static String getInput() {
        //hasInput = false;
        Log.d(TAG, "用户的回答是："+input);
        return input;
    }
    public static void resetInput(){
        input = null;
    }

    public static void isHasInput(boolean hasInput){
        ShareData.hasInput = hasInput;
        Log.d(TAG, "ShareData是否有值"+hasInput);
    }
    public static boolean isInput(){
        synchronized (ShareData.class){
            return hasInput;
        }
    }

    public static void startSpeaking(boolean isSpeakingOrNot){
        ShareData.isSpeakingOrNot = isSpeakingOrNot;
        Log.d(TAG, "startSpeaking()"+isSpeakingOrNot);
    }
    public static boolean isSpeaking(){
        synchronized (ShareData.class){
            return isSpeakingOrNot;
        }
    }

    public static long startTime(long time){
        return time;
    }
    public static void endTime(String time){
        currentTime = time;
    }
    public static String getTime(){
        return currentTime;
    }

    public static void startTimeMills(long time){
        startTime = time;
    }
    public static long getStartTimeMills(){
        Log.d(TAG, "开始讲话的时间："+startTime);
        return startTime;
    }
    public static void endTimeMills (long time){
        endTime = time;
    }
    public static long getEndTimeMills(){
        Log.d(TAG, "结束讲话的时间："+endTime);
        return endTime;
    }

    public static void setStartTimeRob(long time){
        startTimeRob = time;
    }

    public static long getStartTimeRob(){
        return startTimeRob;
    }
    public static void setEndTimeRob(long time){
        endTimeRob = time;
    }
    public static long getEndTimeRob(){
        return endTimeRob;
    }



    public static float duration(){
        Log.d(TAG,"用户沉默的时间为："+(getStartTimeMills() - getEndTimeMills())+"毫秒");
        return (getStartTimeMills() - getEndTimeMills())/1000;
    }

    public static float durationTotal(){
        Log.d(TAG, "用户讲话的总时间："+(getEndTimeRob()-getStartTimeRob())+"毫秒");
        return (getEndTimeRob()-getStartTimeRob())/1000;
    }
}
