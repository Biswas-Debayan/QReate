package com.example.qreate;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Objects.requireNonNull(getSupportActionBar()).hide();

        Button buttonGenerateQR = findViewById(R.id.buttonGenerateQR);
        Button buttonScanQR = findViewById(R.id.buttonScanQR);

        buttonGenerateQR.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, GenerateQrActivity.class);
                startActivity(intent);
            }
        });

        buttonScanQR.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ScanQrActivity.class);
                startActivity(intent);
            }
        });
    }
}
