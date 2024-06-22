package com.example.speecktotext2;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.speecktotext2.D.ShareData;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.FileReader;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SignUpActivity extends AppCompatActivity {
    private FirebaseAuth auth;
    private EditText signupEmail, signupPassword;
    private Button signupButton;
    private TextView loginRedirectText;
    private static final String TAG = LoginActivity.class.getSimpleName();
    private String username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        auth = FirebaseAuth.getInstance();
        signupEmail = findViewById(R.id.signup_email);
        signupPassword = findViewById(R.id.signup_password);
        signupButton = findViewById(R.id.signup_button);
        loginRedirectText = findViewById(R.id.loginRedirectText);

        signupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String user = signupEmail.getText().toString();
                String pass = signupPassword.getText().toString();

                int atIndex = user.indexOf('@');
                if (atIndex != -1){
                    username = user.substring(0, atIndex);
                    Log.d(TAG, "用户名："+username);
                }

                ShareData.setUsername(username);

                if (user.isEmpty()){
                    signupEmail.setError("Email cannot be empty");
                }
                if (pass.isEmpty()){
                    signupPassword.setError("Password cannot be empty");
                }else {
                    savePass(pass);
                    auth.createUserWithEmailAndPassword(user, pass).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()){
                                Toast.makeText(SignUpActivity.this, "SignUp Successfully", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(SignUpActivity.this, MainActivity.class));
                            }else {
                                Toast.makeText(SignUpActivity.this, "SignUp fialed"+task.getException(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            }
        });
        loginRedirectText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
            }
        });
    }

    /**
     * Save password
      * @param text
     */
    public void savePass(String text){
        byte[] messageBytes = new byte[0];
        try {
            messageBytes = text.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        String name = ShareData.getUsername();
        StorageReference storageReference = FirebaseStorage.getInstance().getReference().child(name).child("password.txt");
        //Upload the message
        UploadTask uploadTask = storageReference.putBytes(messageBytes);
        uploadTask.addOnSuccessListener(taskSnapshot -> {
            //Handle successful upload
            storageReference.getDownloadUrl().addOnSuccessListener(uri1 -> {
                String downloadUrl = uri1.toString();
                Log.d(TAG, "密码存储的路径为："+downloadUrl);
            });
        }).addOnFailureListener(exception ->{
            Log.d(TAG, "密码存储失败");
        });
    }
}