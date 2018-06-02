package com.example.wilmersuarez.swaheda;

import android.app.ProgressDialog;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;

import java.util.Objects;

public class RegisterActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private DatabaseReference userDefaultDataReference;
    private EditText registerUsername, registerEmail, registerPassword;
    private ProgressDialog loadingBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Firebase Authentication
        mAuth = FirebaseAuth.getInstance();

        // Toolbar Setup
        Toolbar toolbar = findViewById(R.id.signup_toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setTitle("Sign Up");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // User info setup
        registerUsername = findViewById(R.id.registerName_id);
        registerEmail = findViewById(R.id.registerEmail_id);
        registerPassword = findViewById(R.id.registerPassword_id);
        Button createAccountBtn = findViewById(R.id.registerAccountBtn_id);
        loadingBar = new ProgressDialog(this);

        createAccountBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String userName = registerUsername.getText().toString();
                String email = registerEmail.getText().toString();
                String pw = registerPassword.getText().toString();

                // Create the user and register account
                registerAccount(userName, email, pw);
            }
        });

    }

    // Create the user and register account
    void registerAccount(final String userName, String email, String pw) {
        // Check if fields are valid (Fields are not empty when button is pressed)
        if(TextUtils.isEmpty(userName)) {
            Toast.makeText(RegisterActivity.this, "Name is missing", Toast.LENGTH_SHORT).show();
        } else if(TextUtils.isEmpty(email)) {
            Toast.makeText(RegisterActivity.this, "E-mail is missing", Toast.LENGTH_SHORT).show();
        } else if(TextUtils.isEmpty(pw)) {
            Toast.makeText(RegisterActivity.this, "Password is missing", Toast.LENGTH_SHORT).show();
        }
        else {
            // Register the user using Firebase email and password
            loadingBar.setTitle("Creating Account");
            loadingBar.setMessage("Please wait...");
            loadingBar.show();
            mAuth.createUserWithEmailAndPassword(email, pw).addOnCompleteListener(RegisterActivity.this, new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    // If account created successfully then store the users data in the firebase database
                    if(task.isSuccessful()) {
                        // Get users Device token
                        String deviceToken = FirebaseInstanceId.getInstance().getToken();
                        // Gets the current user UUID
                        String currentUser_Id = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
                        // Create reference to firebase database root for user
                        userDefaultDataReference = FirebaseDatabase.getInstance().getReference().child("Users").child(currentUser_Id);
                        userDefaultDataReference.child("user_name").setValue(userName);
                        userDefaultDataReference.child("user_image").setValue("default_profile_picture");
                        userDefaultDataReference.child("device_token").setValue(deviceToken);
                        userDefaultDataReference.child("user_thumb_image").setValue("default_image")
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        // Validate if data was successfully stored in the firebase database
                                        if(task.isSuccessful()) {
                                            // If successfully stored data, send user to the MainActivity
                                            Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                            startActivity(intent);
                                            finish();
                                        }
                                    }
                                });
                    } else {
                        Toast.makeText(RegisterActivity.this, "Registration Failed! Try again...", Toast.LENGTH_SHORT).show();
                    }
                    loadingBar.dismiss();
                }
            });
        }
    }
}
