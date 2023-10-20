package ru.accesstech.opendooors;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

public class Splash_Screen extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(Splash_Screen.this, AuthorizationScreen.class);
                //Start Authorization Screen
                Splash_Screen.this.startActivity(intent);
                //Stop Service Screen
                Splash_Screen.this.finish();
            }
        }, 2000);
    }
}