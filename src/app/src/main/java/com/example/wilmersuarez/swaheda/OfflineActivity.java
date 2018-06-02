package com.example.wilmersuarez.swaheda;

import android.app.Application;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.OkHttp3Downloader;
import com.squareup.picasso.Picasso;

public class OfflineActivity extends Application {

    private DatabaseReference userRef;

    @Override
    public void onCreate() {
        super.onCreate();

        // Load all string type variables from database
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);

        // Picasso offline picture loading
        Picasso.Builder builder = new Picasso.Builder(this);
        builder.downloader(new OkHttp3Downloader(this, Integer.MAX_VALUE));
        Picasso built = builder.build();
        built.setIndicatorsEnabled(true);
        built.setLoggingEnabled(true);
        Picasso.setSingletonInstance(built);

        // Firebase setup
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        // Get the current user that is online
        FirebaseUser currentUser = mAuth.getCurrentUser();

        // If user is logged in
        if(currentUser != null) {
            String userOnlineId = mAuth.getCurrentUser().getUid();
            userRef = FirebaseDatabase.getInstance().getReference()
                    .child("Users").child(userOnlineId);

            userRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    // If user closes the app, user offline
                    userRef.child("online").onDisconnect().setValue(ServerValue.TIMESTAMP);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }
    }
}
