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
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;

import java.util.Objects;

public class StartPageActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private EditText login_email, login_pw;
    private ProgressDialog loadingBar;

    private DatabaseReference userRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_page);

        // Database reference setup
        userRef = FirebaseDatabase.getInstance().getReference().child("Users");

        // Firebase Authentication
        mAuth = FirebaseAuth.getInstance();

        // Edit text setup
        login_email = findViewById(R.id.email_login_id);
        login_pw = findViewById(R.id.pw_login_id);

        // Button setup
        Button signinBtn = findViewById(R.id.signinBtn);
        Button signupBtn = findViewById(R.id.signupBtn);

        // Toolbar Setup
        Toolbar toolbar = findViewById(R.id.login_toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setTitle("Sign In");

        // If signup button is pressed, go to registerActivity and create account
        signupBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent registerIntent = new Intent(StartPageActivity.this, RegisterActivity.class);
                startActivity(registerIntent);
            }
        });

        loadingBar = new ProgressDialog(this);
        // If sign in button is pressed, sign the user in from the data input by the user
        signinBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = login_email.getText().toString();
                String password = login_pw.getText().toString();

                LoginUser(email, password);
            }
        });
    }

    private void LoginUser(String email, String password) {
        // Validate login fields
        if(TextUtils.isEmpty(email)) {
            Toast.makeText(StartPageActivity.this, "E-mail is missing", Toast.LENGTH_SHORT).show();
        } else if(TextUtils.isEmpty(password)) {
            Toast.makeText(StartPageActivity.this, "Password is missing", Toast.LENGTH_SHORT).show();
        } else {
            // If login fields validation is successful
            loadingBar.setTitle("Signing In");
            loadingBar.setMessage("Please wait...");
            loadingBar.show();
            mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    // if login was successful
                    if (task.isSuccessful()) {
                        String onlineUserId = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
                        String deviceToken = FirebaseInstanceId.getInstance().getToken();

                        // Save the current users Device token (when singed in)
                        userRef.child(onlineUserId).child("device_token").setValue(deviceToken)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                // Send user to main activity
                                Intent loginIntent = new Intent(StartPageActivity.this, MainActivity.class);
                                loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(loginIntent);
                                finish();
                            }
                        });
                    } else {
                        Toast.makeText(StartPageActivity.this, "Login Failed! Try again...", Toast.LENGTH_SHORT).show();
                    }
                    loadingBar.dismiss();
                }
            });
        }
    }
}
