package com.example.wilmersuarez.swaheda;

import android.content.Intent;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private DatabaseReference userRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // When user enters app, it will check if user is signed in.
        mAuth = FirebaseAuth.getInstance(); // Initialize mAuth
        // Get current user
        currentUser = mAuth.getCurrentUser();
        // If online, get reference to user node in database
        if(currentUser != null) {
            String onlineUserId = mAuth.getCurrentUser().getUid();
            userRef = FirebaseDatabase.getInstance().getReference()
                    .child("Users").child(onlineUserId);
        }

        // Toolbar Setup
        android.support.v7.widget.Toolbar toolbar = findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Swaheda");

        // Tab setup
        ViewPager mViewPager = findViewById(R.id.main_tabs_pager);
        TabsPagerAdapter mTabsPagerAdapter = new TabsPagerAdapter(getSupportFragmentManager());
        mViewPager.setAdapter(mTabsPagerAdapter);
        TabLayout mTabsLayout = findViewById(R.id.main_tabs_id);
        mTabsLayout.setupWithViewPager(mViewPager);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Get the current user from firebase database and store in currentUser
        currentUser = mAuth.getCurrentUser();

        // If user is not logged in go to Start Page Activity to login or create account
        if (currentUser == null) {
            LogOutUser();
        } else {
            // If user is online, online value in database becomes true
            userRef.child("online").setValue("true");
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        // If user minimizes that app, set online value to the tim Server Timestamp, to see when
        // they were last online
        if(currentUser != null){
            userRef.child("online").setValue(ServerValue.TIMESTAMP);
        }
    }

    private void LogOutUser() {
        Intent startPageIntent = new Intent(MainActivity.this, StartPageActivity.class);
        // Add flags to main activity to prevent user from returning to MainActivity after being
        // sent to startPageActivity
        startPageIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(startPageIntent);
        finish(); // execute onDestroy
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // If log Out is selected in the menu, log the user out of firebase.
        // and send user to Start Page Activity
        if(item.getItemId() == R.id.main_logout) {
            // If online
            if(currentUser != null) {
                userRef.child("online").setValue(ServerValue.TIMESTAMP);
            }
            mAuth.signOut();
            LogOutUser();
        }

        if(item.getItemId() == R.id.main_account_settings) {
            // If account setting is selected in the menu, send user to Settings Activity
            Intent settingIntent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(settingIntent);
        }

        if(item.getItemId() == R.id.main_all_users) {
            // If All users is selected in the menu, send user to AllUsersActivity
            Intent allUsersIntent = new Intent(MainActivity.this, AllUsersActivity.class);
            startActivity(allUsersIntent);
        }
        return true;
    }
}
