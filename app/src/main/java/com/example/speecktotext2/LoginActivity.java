package com.example.speecktotext2;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.speecktotext2.C.MicActivity;
import com.example.speecktotext2.D.ShareData;
import com.example.speecktotext2.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = LoginActivity.class.getSimpleName();
    private FirebaseAuth auth;
    private EditText loginEmail, loginPassword;
    private TextView signupRedirectText;
    private Button loginButton;
    private String username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();
        loginEmail = findViewById(R.id.login_email);
        //loginPassword = findViewById(R.id.login_password);
        loginButton = findViewById(R.id.login_button);
        signupRedirectText = findViewById(R.id.signupRedirectText);

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null){
            String userEmail = currentUser.getEmail();
            loginEmail.setText(userEmail);
        }

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = loginEmail.getText().toString();
                //String pass = loginPassword.getText().toString();

                int atIndex = email.indexOf('@');
                if (atIndex != -1){
                    username = email.substring(0, atIndex);
                    Log.d(TAG, "用户名："+username);
                }
                ShareData.setUsername(username);

                if (!email.isEmpty()&& Patterns.EMAIL_ADDRESS.matcher(email).matches()){
                    //Log.d(TAG, ShareData.getPass());
                    auth.signInWithEmailAndPassword(email, "123456")
                            .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                                @Override
                                public void onSuccess(AuthResult authResult) {
                                    Toast.makeText(LoginActivity.this, "Login Successful", Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                    finish();
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Toast.makeText(LoginActivity.this, "Login Failed", Toast.LENGTH_SHORT).show();
                                }
                            });

                    /*if (!pass.isEmpty()){

                    }else {
                        loginPassword.setError("Password cannot be empty");
                    }*/
                } else if (email.isEmpty()) {
                    loginEmail.setError("Email cannot be empty");
                }else {
                    loginEmail.setError("Please enter valid email");
                }
            }
        });
        signupRedirectText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, SignUpActivity.class));
            }
        });
        retrievePass();
    }

    /**
     * Retrieve user's password from Firebase storage
     */
    public void retrievePass(){
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        FirebaseUser currentUser = auth.getCurrentUser();
        if(currentUser != null){
            String userEmail = currentUser.getEmail();
            int atIndex = userEmail.indexOf('@');
            if (atIndex != -1){
                String name = userEmail.substring(0, atIndex);
                ShareData.setUsername(name);

                StorageReference fileRef = storageRef.child(ShareData.getUsername()).child("password.txt");
                fileRef.getBytes(Long.MAX_VALUE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                    @Override
                    public void onSuccess(byte[] bytes) {
                        String password = new String(bytes);
                        ShareData.setPass(password);
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "密码获取失败！");
                    }
                });
            }
        }
    }
}