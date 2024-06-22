package com.example.speecktotext2.C;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import com.airbnb.lottie.LottieAnimationView;
import com.example.speecktotext2.Congrats;
import com.example.speecktotext2.D.ShareData;
import com.example.speecktotext2.D.SpeechAPI;
import com.example.speecktotext2.D.TextToSpeechEvent;
import com.example.speecktotext2.D.VoiceRecorder;
import android.Manifest;

import com.example.speecktotext2.R;
import com.example.speecktotext2.Utils.DataClass;
import com.example.speecktotext2.Utils.StringUtils;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.api.client.util.Data;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageException;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import android.app.Activity;
import android.os.Handler;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class MicActivity extends AppCompatActivity {
    private static final String TAG = MicActivity.class.getSimpleName();
    private ListView listView;
    private List<String> list;
    private TextView textView;
    private ArrayAdapter adapter;
    private static final int RECORD_Request_CODE = 101;
    private SpeechAPI speechAPI;
    private VoiceRecorder voiceRecorder;
    private WebView webView;
    private LottieAnimationView lottie;
    TextToSpeech my_tts_object;
    long startTime;
    String  endTime;
    int i = 0;
    StringUtils su = new StringUtils();




    private final VoiceRecorder.Callback callback = new VoiceRecorder.Callback() {
        @Override
        public void onVoiceStart() {
            if (speechAPI != null){
                speechAPI.startRecognizing(voiceRecorder.getSampleRate());
                Log.d(TAG, "speechAPI.startRecognizing(voiceRecorder.getSampleRate());");
            }
        }

        @Override
        public void onVoice(byte[] data, int size) {
            if (speechAPI != null){
                speechAPI.recognize(data, size);
            }
        }

        @Override
        public void onVoiceEnd() {
            if (speechAPI != null){
                speechAPI.finishRecognizing();
                Log.d(TAG, "speechAPI.finishRecognizing()");
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.c_mic_activity);

        textView = findViewById(R.id.textView);
        listView = findViewById(R.id.listview);
        list = new ArrayList<>();
        adapter = new ArrayAdapter(this, android.R.layout.simple_expandable_list_item_1, list);
        listView.setAdapter(adapter);
        Log.d(TAG, "Debug!");
        listView.setVerticalScrollBarEnabled(false);

        speechAPI = new SpeechAPI(this);
        Log.d(TAG, "SpeechAPI的对象是："+this);
        initializeTextToSpeech();
        EventBus.getDefault().register(this);

        webView = findViewById(R.id.webView);
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webView.setLayerType(WebView.LAYER_TYPE_HARDWARE, null);
        webView.loadUrl("file:///android_asset/waveWaiting.html");

        lottie = findViewById(R.id.lottie);
        lottie.playAnimation();

        //finishTalk();
    }
    private int GrantedPermission(String permission){
        return ContextCompat.checkSelfPermission(this, permission);
    }

    private void makeRequest(String permission){
        ActivityCompat.requestPermissions(this, new String[]{permission}, RECORD_Request_CODE);
    }

    private void initializeTextToSpeech(){
        //语音合成初始化
        my_tts_object = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                //Check the Initialization of TTS Engine
                if(status == TextToSpeech.SUCCESS){
                    Log.d(TAG, "my_tts_object初始化成功");
                    //set the language
                    int result = my_tts_object.setLanguage(Locale.US);
                    if (result == TextToSpeech.LANG_MISSING_DATA
                            || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("MESSAGE", "不支持的语言");
                    }
                }else {
                    Log.e("MESSAGE", "TextToSpeech initialization failed");
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == RECORD_Request_CODE) {
            if (grantResults.length == 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                finish();
            } else {
                startVoiceRecorder();
                Log.d(TAG,"startVoiceRecorder();");
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onTextToSpeechEvent(TextToSpeechEvent event) throws IOException {
        String message = event.getMessage();
        textToSpeech(message);
    }

    public void textToSpeech(String text) throws IOException {
        String questionStore = "";
        utterance();
        Log.d(TAG, "textToSpeech获取到的值："+text);
        if (su.searchMatchStart(text)){
            Log.d(TAG,"su.searchMatchStart(text):"+su.searchMatchStart(text));
            Log.d(TAG,"第一个问题是："+su.question(0));
            HashMap<String, String> params = new HashMap<>();
            params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,"utteranceID");
            my_tts_object.speak(su.question(0), TextToSpeech.QUEUE_FLUSH,params);
            questionStore = su.question(0);
            Log.d(TAG, "问题是：" + su.questionNo(0));
        }else {
            if (su.searchMatchSorry(text)){//没听懂
                HashMap<String, String> params = new HashMap<>();
                params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,"utteranceID");
                my_tts_object.speak(su.lastQuestion(i), TextToSpeech.QUEUE_FLUSH,params);
                questionStore = su.lastQuestion(i);
            }else if (su.searchMatchNo(text)){//否定回答
                HashMap<String, String> params = new HashMap<>();
                params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,"utteranceID");
                my_tts_object.speak(su.questionNo(i), TextToSpeech.QUEUE_FLUSH,params);
                Log.d(TAG, "问题是：" + su.questionNo(i));
                questionStore = su.questionNo(i);
                i++;
                Log.d(TAG, "第几个问题：" + i);
            } else if (su.searchMatchYes(text)) { //肯定回答
                HashMap<String, String> params = new HashMap<>();
                params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,"utteranceID");
                my_tts_object.speak(su.questionYes(i), TextToSpeech.QUEUE_FLUSH,params);
                Log.d(TAG, "问题是：" + su.questionYes(i));
                questionStore = su.questionYes(i);
                i++;
                Log.d(TAG, "第几个问题：" + i);
            } else if (su.questionHasTime(text)) { //匹配时间
                HashMap<String, String> params = new HashMap<>();
                params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,"utteranceID");
                my_tts_object.speak(su.questionYes(i-1), TextToSpeech.QUEUE_FLUSH,params);
                Log.d(TAG, "问题是：" + su.questionYes(i-1));
                questionStore = su.questionYes(i-1);
                i++;
                Log.d(TAG, "第几个问题：" + i);
            } else if (su.searchMatchPeo(text)) {  //匹配人
                HashMap<String, String> params = new HashMap<>();
                params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,"utteranceID");
                my_tts_object.speak(su.question(1), TextToSpeech.QUEUE_FLUSH,params);
                Log.d(TAG, "问题是：" + su.question(1));
                questionStore = su.question(1);
                i++;
                Log.d(TAG, "第几个问题：" + i);
            } else if (su.searchMatchNum(text)) { //匹配数字
                HashMap<String, String> params = new HashMap<>();
                params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,"utteranceID");
                my_tts_object.speak(su.questionYes(5), TextToSpeech.QUEUE_FLUSH,params);
                Log.d(TAG, "问题是：" + su.questionYes(5));
                i = 5;
                i++;
                Log.d(TAG, "第几个问题：" + i);
            } else if (!su.searchMatchSorry(text)&& !su.searchMatchNo(text) &&
                    !su.searchMatchYes(text)&&!su.questionHasTime(text)&&!su.searchMatchPeo(text)&&!su.searchMatchNum(text)&&
                    !su.searchMatchEnd(text)&&!su.searchMatchStart(text)) {
                HashMap<String, String> params = new HashMap<>();
                params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,"utteranceID");
                my_tts_object.speak(su.questionYes(i), TextToSpeech.QUEUE_FLUSH,params);
                Log.d(TAG, "问题是：" + su.questionYes(i));
                questionStore = su.questionYes(i);
                i++;
                Log.d(TAG, "第几个问题：" + i);
            }
        }
         //数据存储
        SimpleDateFormat sdf = new SimpleDateFormat("DD-MM-yyyy HH:MM:ss", Locale.getDefault());
        String currentDataAndTime = sdf.format(new Date());//机器人讲话的结束时间
        ShareData.setEndTimeRob(System.currentTimeMillis());
        String message = "kept silent for:" +ShareData.duration() +"s" + "\n"
                +ShareData.getTime()+ " " + "Child: " + text + "\n"
                + currentDataAndTime+ " " + "Dozzz: " + questionStore + "\n"
                + "answered this question for:" + ShareData.durationTotal() + "s" +"\n";
        uploadData(text);
        saveData(message);
        Log.d(TAG, "机器人已提问");
    }
    private void utterance() {
        my_tts_object.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
                ShareData.startSpeaking(true);
                System.out.println("SpeechStart");
                startTime = ShareData.startTime(System.currentTimeMillis());
                System.out.println("线程开始运行的时间为"+startTime);
            }
            @Override
            public void onDone(String utteranceId) {
                ShareData.startSpeaking(false);
                ShareData.isHasInput(false);
                Log.d(TAG,"ShareData是否有值："+ShareData.isInput());
                System.out.println("SpeechCompleted");

                SimpleDateFormat sdf = new SimpleDateFormat("DD-MM-yyyy HH:MM:ss", Locale.getDefault());
                String currentDataAndTime = sdf.format(new Date());

                ShareData.endTime(currentDataAndTime);//机器人讲话的结束时间
                ShareData.endTimeMills(System.currentTimeMillis());
                ShareData.setStartTimeRob(System.currentTimeMillis());
                System.out.println("线程结束运行的时间为:"+ currentDataAndTime);
            }
            @Override
            public void onError(String utteranceId) {
                System.out.println("SpeechERROR");
            }
        });
    }

    /**
     * Data storage at Firebase
     * @param text
     */
    public void saveData(String text) throws IOException {

        String name = ShareData.getUsername();
        Log.d(TAG, "文件夹名："+name);
        StorageReference storageReference = FirebaseStorage.getInstance().getReference().child(name).child("message1.txt");
        byte[] fileData = "Hello, this is the content of conversation!\n\n".getBytes();
        UploadTask uploadTask = storageReference.putBytes(fileData);
        uploadTask.addOnSuccessListener(taskSnapshot -> {
            //File upload successful
            Log.d(TAG, "File upload successfully.");
        }).addOnFailureListener(exception -> {
            // Handle file upload failure
            Log.e(TAG, "Error uploading file: " + exception.getMessage(), exception);
        });

        storageReference.getMetadata().addOnSuccessListener(metadata -> {
            // The file exists, proceed with your operations
        }).addOnFailureListener(exception -> {
            // The file does not exist, handle the error
            if (exception instanceof StorageException && ((StorageException) exception).getErrorCode() == StorageException.ERROR_OBJECT_NOT_FOUND) {
                // File not found
                Log.e(TAG, "File not found");
            } else {
                // Handle other errors
                Log.e(TAG, "Error getting metadata: " + exception.getMessage(), exception);
            }
        });

        File localFile = File.createTempFile("message1", "txt");
        storageReference.getFile(localFile)
                .addOnSuccessListener(taskSnapshot -> {
                    // File downloaded successfully
                    // Now you can append new data to this file
                    appendDataToFile(localFile, text);
                })
                .addOnFailureListener(exception -> {
                    // Handle errors here (e.g., file doesn't exist, download failed)
                    Log.d(TAG, "文件不存在");
                });
    }

    private void appendDataToFile(File file, String newData){
        try {
            FileWriter writer = new FileWriter(file, true);
            //Append the new data
            writer.write(String.valueOf(newData));
            writer.write("\n");
            writer.close();
            uploadModifiedFile(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void uploadModifiedFile(File file){
        String name = ShareData.getUsername();
        StorageReference storage = FirebaseStorage.getInstance().getReference().child(name).child("message1.txt");

        Uri fileUri = Uri.fromFile(file);

        storage.putFile(fileUri)
                .addOnSuccessListener(taskSnapshot -> {
                    // File uploaded successfully
                    Log.d(TAG, "文件更新成功");
                })
                .addOnFailureListener(exception -> {
                    // Handle errors here (e.g., upload failed)
                    Log.d(TAG, "文件更新失败");
                });
    }

    /**
     * Data update realtime
     * @param text
     */
    public void uploadData(String text){
        Log.d(TAG, "子文件夹的名字："+text);
        DataClass dataClass = new DataClass(text);
        FirebaseDatabase.getInstance().getReference("Children's answer").child(text)
                .setValue(dataClass).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()){
                            Log.d(TAG, "Answer store successfully.");
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "Answer store failed.");
                    }
                });
    }

    private final SpeechAPI.Listener listener = new SpeechAPI.Listener() {
        @Override
        public void onSpeechRecognized(String text, boolean isFinal) {
            if (isFinal){
                voiceRecorder.dismiss();
            }
            if (textView != null && !TextUtils.isEmpty(text)){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (isFinal){
                            //textView.setText(null);
                            textView.setVisibility(View.VISIBLE);
                            Log.d(TAG,"编译的文字为1："+text);
                        }else {
                            textView.setText(text);
                            Log.d(TAG,"编译的文字为2："+text);
                            ShareData.startTimeMills(System.currentTimeMillis());
                            Log.d(TAG, "用户开始讲话的时间是："+System.currentTimeMillis());
                            textView.setVisibility(View.VISIBLE);
                        }

                    }
                });
            }
        }
    };

    /**
     * Wave view
     */
    private final SpeechAPI.Listener listener2 = new SpeechAPI.Listener() {
        @Override
        public void onSpeechRecognized(String text, boolean isFinal) {
            if (textView != null && !TextUtils.isEmpty(text)){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (isFinal) {
                            webView.loadUrl("file:///android_asset/waveWaiting.html");
                        } else {
                            //波纹展示
                            webView.loadUrl("file:///android_asset/waveStart.html");
                        }
                    }
                });
            }
        }
    };

    /**
     * When the talking finished, jump to the Congrats!
     */
    private final SpeechAPI.Listener listener3 = new SpeechAPI.Listener() {
        @Override
        public void onSpeechRecognized(String text, boolean isFinal) {
            if (textView != null && !TextUtils.isEmpty(text)){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (i >= 9 && su.searchMatchEnd(text)){
                            startActivity(new Intent(MicActivity.this, Congrats.class));
                            Log.d(TAG, "结束聊天");
                        }
                    }
                });
            }
        }
    };

    private void startVoiceRecorder(){
        if (voiceRecorder != null){
            voiceRecorder.stop();
        }
        voiceRecorder = new VoiceRecorder(callback);
        voiceRecorder.start();
        Log.d(TAG, " voiceRecorder.start();");
    }
    private void stopVoiceRecorder(){
        if (voiceRecorder != null){
            voiceRecorder.stop();
            Log.d(TAG,"voiceRecorder.stop();");
            voiceRecorder = null;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (GrantedPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED){
            startVoiceRecorder();
            Log.d(TAG, "onStart中的startVoiceRecorder();");
        }else {
            makeRequest(Manifest.permission.RECORD_AUDIO);
            Log.d(TAG, "makeRequest(Manifest.permission.RECORD_AUDIO);");
        }
        speechAPI.addListener(listener);
        Log.d(TAG,"执行speechAPI.addListener(listener);" + listener);
        speechAPI.addListener(listener2);
        speechAPI.addListener(listener3);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG,"onResume()");
    }

    @Override
    protected void onStop() {
        stopVoiceRecorder();
        speechAPI.removeListener(listener);
        speechAPI.removeListener(listener2);
        speechAPI.removeListener(listener3);
        speechAPI.destroy();
        speechAPI = null;
        EventBus.getDefault().unregister(this);
        super.onStop();
    }
}