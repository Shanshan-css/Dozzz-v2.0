package com.example.speecktotext2.D;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;

import org.greenrobot.eventbus.EventBus;


public class VoiceRecorder {
    private static final int[] SAMPLE_RATE_CANDIDATES = new int[]{16000, 11025, 2205, 44100};
    private static final int CHANNEL = AudioFormat.CHANNEL_IN_MONO;
    private static final int ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private static final String TAG = VoiceRecorder.class.getSimpleName();

    private static final int AMPLITUDE_THRESHOLD = 1500;

    private static final int SPEECH_TIMEOUT_MILLIS = 1500;
    private static final int MAX_SPEECH_LENGTH_MILLIS = 30 * 1000;

    public static abstract class Callback{
        public void onVoiceStart(){
        }
        public void onVoice(byte[] data, int size){
        }
        public void onVoiceEnd(){
        }
    }
    private final Callback mCallback;
    private AudioRecord mAudioRecord;
    private Thread mThread;
    private byte[] mBuffer;
    private final Object mLock = new Object();
    private long mLastVoiceHearMillis = Long.MAX_VALUE;
    private long mVoiceStartedMillis;


    public VoiceRecorder(@NonNull Callback callback){mCallback = callback;}
    public void start(){
        //stop recording if it is currently ongoing.
        stop();
        //Try to create a new recording session.
        mAudioRecord = createAudioRecord();
        if (mAudioRecord == null){
            throw new RuntimeException("Cannot instantiate VoiceRecorder");
        }
        //Start recording.
        mAudioRecord.startRecording();
        Log.d(TAG,"mAudioRecord.startRecording();");
        //Start processing the capture audio.
        mThread = new Thread(new InputVoice());
        Thread output = new Thread(new OutputVoice());
        Log.d(TAG,"mThread = new Thread(new InputVoice());");
        mThread.start();
        Log.d(TAG, "mThread.start();");
        output.start();
    }
    public void stop(){
        synchronized (mLock){
            dismiss();
            if (mThread != null){
                mThread.interrupt();
                mThread = null;
            }
            if (mAudioRecord!=null){
                mAudioRecord.stop();
                mAudioRecord.release();
                mAudioRecord = null;
            }
            mBuffer = null;
        }
    }
    public void dismiss(){
        if (mLastVoiceHearMillis != Long.MAX_VALUE){
            mLastVoiceHearMillis = Long.MAX_VALUE;
            mCallback.onVoiceEnd();
        }
    }
    public int getSampleRate(){
        if (mAudioRecord != null){
            return mAudioRecord.getSampleRate();
        }
        return 0;
    }
    private AudioRecord createAudioRecord(){
        for (int sampleRate : SAMPLE_RATE_CANDIDATES){
            final int sizeInBytes = AudioRecord.getMinBufferSize(sampleRate, CHANNEL, ENCODING);
            if (sizeInBytes == AudioRecord.ERROR_BAD_VALUE){
                continue;
            }
            final AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    sampleRate, CHANNEL, MediaRecorder.AudioEncoder.AMR_NB, sizeInBytes);
            if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED){
                mBuffer = new byte[sizeInBytes];
                return audioRecord;
            }else {
                audioRecord.release();
            }
        }
        return null;
    }
    private class InputVoice implements Runnable{
        @Override
        public void run() {
            while (true){
                synchronized (mLock){

                    if (Thread.currentThread().isInterrupted()){
                        break;
                    }
                    if (ShareData.isInput()){
                        try {
                            Log.d(TAG,"SharaData的值还未传给输出线程，输入线程等待");
                            mLock.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    final int size = mAudioRecord.read(mBuffer, 0,mBuffer.length);
                    final long now = System.currentTimeMillis();
                    if (isHearingVoice(mBuffer, size)){
                        if (mLastVoiceHearMillis == Long.MAX_VALUE){
                            mVoiceStartedMillis = now;
                            Log.d(TAG,"谷歌接口获取的系统时间："+ mVoiceStartedMillis);
                            Log.d(TAG,"谷歌接口获取的最大时间："+mLastVoiceHearMillis);
                            mCallback.onVoiceStart();
                            Log.d(TAG, "mCallback.onVoiceStart();");
                        }
                        mCallback.onVoice(mBuffer, size);
                        Log.d(TAG,"执行1：mCallback.onVoice(mBuffer, size);");
                        mLastVoiceHearMillis = now;
                        if (now - mVoiceStartedMillis > MAX_SPEECH_LENGTH_MILLIS){
                            end();
                        }
                    } else if (mLastVoiceHearMillis != Long.MAX_VALUE) {
                        mCallback.onVoice(mBuffer, size);
                        Log.d(TAG,"执行2：mCallback.onVoice(mBuffer, size);");
                        if (now - mLastVoiceHearMillis > SPEECH_TIMEOUT_MILLIS){
                            end();
                        }
                    }
                    Log.d(TAG,"InputVoice flag ++");
                    mLock.notify();
                }
            }
        }
        private void end(){
            mLastVoiceHearMillis = Long.MAX_VALUE;
            mCallback.onVoiceEnd();
        }
        private boolean isHearingVoice(byte[] buffer, int size){
            for (int i = 0; i < size - 1; i +=2){
                //The buffer has LINEAR16 in little endian.
                int s = buffer[i + 1];
                if (s < 0) s *= -1;
                s <<= 8;
                s += Math.abs(buffer[i]);
                if (s > AMPLITUDE_THRESHOLD){
                    return true;
                }
            }
            return false;
        }
    }
    class OutputVoice implements Runnable{//输出线程（机器人提问）
        @Override
        public void run() {
            while (true){
                synchronized (mLock){
                    if (!ShareData.isInput()){
                        try {
                            Log.d(TAG,"SharaData的值已传给输出线程，输出线程等待");
                            mLock.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    String text = ShareData.getInput();
                    Log.d(TAG,"输出线程获得到的文字内容为："+text);
                    EventBus.getDefault().post(new TextToSpeechEvent(text));
                    Log.d(TAG,"Eventbus");
                    ShareData.resetInput();
                    /*try {
                        Thread.sleep(1000);
                        Log.d(TAG,"停顿1:"+1000+"毫秒");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }*/
                    for (int j = 0; j<=15; j++){
                        try {
                            Thread.sleep(10);
                            Log.d(TAG,"停顿2:"+10+"毫秒");

                            //Log.d(TAG, "沉默的时间为1："+silentTime+"s");
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        if (!ShareData.isSpeaking()){
                            break;
                        }
                    }
                    Log.d(TAG,"OutputVoice flag ++");
                    mLock.notify();
                }
            }
        }
    }
}
