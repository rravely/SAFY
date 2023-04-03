package com.example.safy;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

//import com.example.camera.databinding.ActivityMainBinding;

public class StartActivity extends AppCompatActivity {

    //private ActivityMainBinding binding;
    private Button btn_start;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        btn_start = findViewById(R.id.btn_start);
        btn_start.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Intent intent = new Intent(StartActivity.this, MainActivity.class);
                startActivity(intent); //액티비티 이동
            }
        });
    }
    public native String stringFromJNI();

}