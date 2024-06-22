package com.example.speecktotext2.D;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.GnssAntennaInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.speecktotext2.R;
import com.example.speecktotext2.Utils.DataClass;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.speech.v1.RecognitionConfig;
import com.google.cloud.speech.v1.SpeechGrpc;
import com.google.cloud.speech.v1.SpeechRecognitionAlternative;
import com.google.cloud.speech.v1.StreamingRecognitionConfig;
import com.google.cloud.speech.v1.StreamingRecognitionResult;
import com.google.cloud.speech.v1.StreamingRecognizeRequest;
import com.google.cloud.speech.v1.StreamingRecognizeResponse;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.protobuf.ByteString;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import android.os.Handler;

import io.grpc.ClientCall;
import io.grpc.ManagedChannel;
import io.grpc.internal.DnsNameResolverProvider;
import io.grpc.okhttp.OkHttpChannelProvider;
import io.grpc.stub.StreamObserver;

public class SpeechAPI {
    public static final List<String> SCOPE =
            Collections.singletonList("https://www.googleapis.com/auth/cloud-platform");
    public static final String TAG = SpeechAPI.class.getSimpleName();

    private static final String PREFS = "SpeechService";
    private static final String PREF_ACCESS_TOKEN_VALUE = "access_token_value";
    private static final String PREF_ACCESS_TOKEN_EXPIRATION_TIME = "access_token_expiration_time";
    private static final int ACCESS_TOKEN_EXPIRATION_TOLERANCE = 30 * 60 * 1000;//thirty minutes
    private static final int ACCESS_TOKEN_FETCH_MARGIN = 60 * 1000; //one minute

    private static final String HOSTNAME = "speech.googleapis.com";
    private static final int PORT = 443;
    private static Handler mHandler;
    private final ArrayList<Listener> mListeners = new ArrayList<>();
    private String voiceInput = null;



    private final StreamObserver<StreamingRecognizeResponse> mResponseObserver =
            new StreamObserver<StreamingRecognizeResponse>() {
                @Override
                public void onNext(StreamingRecognizeResponse response) {
                    String text = null;
                    boolean isFinal = false;
                    Log.d(TAG,"boolean isFinal = false;");
                    if (response.getResultsCount() > 0){
                        Log.d(TAG, "response.getResultsCount() = "+ response.getResultsCount());
                        final StreamingRecognitionResult result = response.getResults(0);
                        Log.d(TAG, "response.getResults:"+result);
                        isFinal = result.getIsFinal();
                        Log.d(TAG,"是否已讲完话："+isFinal);
                        if (result.getAlternativesCount() > 0){
                            Log.d(TAG, "result.getAlternativesCount()=" + result.getAlternativesCount());
                            final SpeechRecognitionAlternative alternative = result.getAlternatives(0);
                            Log.d(TAG, "result.getAlternatives():"+alternative);
                            text = alternative.getTranscript();
                            Log.d(TAG, "SpeechAPI获得的文本1是："+text);
                            isFinal = result.getIsFinal();
                            Log.d(TAG,"是否已讲完话："+isFinal);

                            /*String finalInput = voiceInput;
                            Log.d(TAG, "finalInput = " + finalInput);
                            voiceInput = text;
                            Log.d(TAG, "voiceInput = " + voiceInput);
                            Log.d(TAG, "finalInput与voiceInput是否相等" + voiceInput.equals(finalInput));*/
                            if (isFinal){
                                if (text != null){
                                    ShareData.setInput(text);
                                    Log.d(TAG, "输入的文字1为："+text);
                                }
                            }
                        }
                    }
                    if (text != null){
                        for (Listener listener: mListeners){
                            listener.onSpeechRecognized(text, isFinal);
                            Log.d(TAG, "SpeechAPI获得的文本2是："+text);
                        }
                    }
                }
                @Override
                public void onError(Throwable t) {
                    Log.e(TAG, "Error callong the API.");
                }
                @Override
                public void onCompleted() {
                    Log.i(TAG, "API completed.");
                }
            };
    private Context mContext;
    private volatile AccessTokenTask mAccessTokenTask;
    private final Runnable mFetchAccessTokenRunnable = new Runnable() {
        @Override
        public void run() {
            fetchAccessToken();
            Log.d(TAG,"fetchAccessToken();");
        }
    };
    private SpeechGrpc.SpeechStub mApi;
    private StreamObserver<StreamingRecognizeRequest> mRequestObserver;

    public SpeechAPI(Context mContext){
        this.mContext = mContext;
        mHandler = new Handler();
        fetchAccessToken();
    }

    public void destroy(){
        mHandler.removeCallbacks(mFetchAccessTokenRunnable);
        mHandler = null;
        //Release the gRPC channel.
        if (mApi != null){
            final ManagedChannel channel = (ManagedChannel) mApi.getChannel();
            if (channel != null && !channel.isShutdown()){
                try {
                    channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Log.e(TAG, "Error shutting sown the gRPC channel.", e);
                }
            }
            mApi = null;
        }
    }
    private void fetchAccessToken() {
        if (mAccessTokenTask != null){
            return;
        }
        mAccessTokenTask = new AccessTokenTask();
        mAccessTokenTask.execute();
    }

    public void addListener(@NonNull Listener listener){
        mListeners.add(listener);
        Log.d(TAG, "mListeners.add(listener);");
    }

    public void removeListener(@NonNull Listener listener){ mListeners.remove(listener);}
    public void startRecognizing(int sampleRate){
        if (mApi == null){
            Log.w(TAG, "API not ready. Ignoring the request.");
            return;
        }
        //Configure the API
        mRequestObserver = mApi.streamingRecognize(mResponseObserver);
        Log.d(TAG,"mRequestObserver = mApi.streamingRecognize(mResponseObserver);");

        StreamingRecognitionConfig streamingConfig = StreamingRecognitionConfig.newBuilder()
                .setConfig(RecognitionConfig.newBuilder()
                        .setLanguageCode("en-US")
                        .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                        .setSampleRateHertz(sampleRate)
                        .build()
                )
                .setInterimResults(true)
                .setSingleUtterance(true)
                .build();

        StreamingRecognizeRequest streamingRecognizeRequest = StreamingRecognizeRequest.newBuilder().setStreamingConfig(streamingConfig).build();
        mRequestObserver.onNext(streamingRecognizeRequest);
        Log.d(TAG, "mRequestObserver.onNext(streamingRecognizeRequest);");
    }
    public void recognize(byte[] data, int size){
        if (mRequestObserver == null){
            return;
        }
        //Call the streaming recognition API
        mRequestObserver.onNext(StreamingRecognizeRequest.newBuilder()
                .setAudioContent(ByteString.copyFrom(data, 0, size))
                .build());
        Log.d(TAG,"recognize");
    }
    public void finishRecognizing(){
        if (mRequestObserver == null){
            return;
        }
        mRequestObserver.onCompleted();
        Log.d(TAG, "mRequestObserver.onCompleted();");
        mRequestObserver = null;
    }
    public interface Listener {
        //Called when a new piece of text wes recognized by the Speech API.
        void onSpeechRecognized(String text, boolean isFinal);
    }
    private class AccessTokenTask extends AsyncTask<Void, Void, AccessToken>{
        @Override
        protected AccessToken doInBackground(Void... voids) {
            final SharedPreferences prefs = mContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            String tokenValue = prefs.getString(PREF_ACCESS_TOKEN_VALUE, null);
            long expirationTime = prefs.getLong(PREF_ACCESS_TOKEN_EXPIRATION_TIME, -1);

            //Check if the current token is still valid for a while
            if (tokenValue != null && expirationTime > 0){
                if (expirationTime > System.currentTimeMillis() + ACCESS_TOKEN_EXPIRATION_TOLERANCE){
                    return new AccessToken(tokenValue, new Date(expirationTime));
                }
            }
            try {
                final InputStream stream = mContext.getResources().openRawResource(R.raw.credential);
                final GoogleCredentials credentials = GoogleCredentials.fromStream(stream).createScoped(SCOPE);
                final AccessToken token = credentials.refreshAccessToken();
                prefs.edit()
                        .putString(PREF_ACCESS_TOKEN_VALUE, token.getTokenValue())
                        .putLong(PREF_ACCESS_TOKEN_EXPIRATION_TIME, token.getExpirationTime().getTime())
                        .apply();
                return token;
            } catch (IOException e) {
                Log.e(TAG,"Failed to obtain access token.", e);
            }
            return null;
        }
        protected void onPostExecute(AccessToken accessToken){
            mAccessTokenTask = null;
            final ManagedChannel channel = new OkHttpChannelProvider()
                    .builderForAddress(HOSTNAME, PORT)
                    .nameResolverFactory(new DnsNameResolverProvider())
                    .intercept(new Credentials(new GoogleCredentials(accessToken)
                            .createScoped(SCOPE)))
                    .build();
            mApi = SpeechGrpc.newStub(channel);

            //Schedule access token refresh before it expires
            if (mHandler != null){
                mHandler.postDelayed(mFetchAccessTokenRunnable,
                        Math.max(accessToken.getExpirationTime().getTime() - System.currentTimeMillis() - ACCESS_TOKEN_EXPIRATION_TOLERANCE, ACCESS_TOKEN_EXPIRATION_TOLERANCE));
            }
        }
    }
}
