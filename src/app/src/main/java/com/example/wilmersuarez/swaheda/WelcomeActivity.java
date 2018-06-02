package com.example.wilmersuarez.swaheda;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ProgressBar;

public class WelcomeActivity extends AppCompatActivity {

    ImageView appName, bubble;
    Animation fromBottom, fromTop;
    ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        // Splash Screen animation setup
        appName = findViewById(R.id.logo_name);
        bubble = findViewById(R.id.bubble_id);
        progressBar = findViewById(R.id.progressBar);
        fromBottom = AnimationUtils.loadAnimation(this, R.anim.frombottom);
        fromTop = AnimationUtils.loadAnimation(this, R.anim.fromtop);

        appName.setAnimation(fromBottom);
        bubble.setAnimation(fromTop);

        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    sleep(3000);    // Keep the Welcome Page visible for 3 seconds
                } catch(Exception e) {
                    e.printStackTrace();
                } finally {
                    Intent mainIntent = new Intent(WelcomeActivity.this, MainActivity.class);
                    startActivity(mainIntent);
                }
            }
        };
        thread.start();
    }

    @Override
    protected void onPause() {
        super.onPause();

        finish();   // execute onDestroy
    }
}
