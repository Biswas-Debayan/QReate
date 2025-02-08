package com.example.qreate;

import androidx.appcompat.app.AppCompatActivity;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class GenerateQrActivity extends AppCompatActivity {

    private EditText editTextTitle;
    private EditText editTextDescription;
    private ImageView imageViewQrCode;
    private String encryptedString;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generate_qr);
        Objects.requireNonNull(getSupportActionBar()).hide();

        editTextTitle = findViewById(R.id.editTextTitle);
        editTextDescription = findViewById(R.id.editTextDescription);
        Button buttonSubmit = findViewById(R.id.buttonSubmit);
        imageViewQrCode = findViewById(R.id.imageViewQrCode);

        buttonSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String title = editTextTitle.getText().toString().trim();
                String description = editTextDescription.getText().toString().trim();

                // Combine data (you can format this as needed, like JSON)
                String data = "Title: " + title + "\nDescription: " + description;

                // Encrypt the data
                String password = "QReate1234567890"; // 16 characters for AES-128
                try {
                    encryptedString = encrypt(password, data);
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e("Encryption Error", "Error encrypting data: " + e.getMessage());
                    Toast.makeText(GenerateQrActivity.this, "Encryption failed", Toast.LENGTH_SHORT).show();
                    return; // Stop if encryption fails
                }

                // Generate QR code
                WindowManager manager = (WindowManager) getSystemService(WINDOW_SERVICE);
                Display display = manager.getDefaultDisplay();
                Point point = new Point();
                display.getSize(point);
                int width = point.x;
                int height = point.y;
                int smallerDimension = Math.min(width, height);
                smallerDimension = smallerDimension * 3 / 4;

                try {
                    Bitmap bitmap = encodeAsBitmap(encryptedString, smallerDimension);
                    imageViewQrCode.setImageBitmap(bitmap);
                } catch (WriterException e) {
                    e.printStackTrace();
                    Toast.makeText(GenerateQrActivity.this, "QR Code generation failed", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private String encrypt(String password, String data) throws Exception {
        SecretKeySpec secretKey = new SecretKeySpec(password.getBytes(StandardCharsets.UTF_8), "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encryptedBytes = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return Base64.getEncoder().encodeToString(encryptedBytes);
        }
        return password;
    }

    private Bitmap encodeAsBitmap(String str, int dimension) throws WriterException {
        BitMatrix result;
        try {
            result = new MultiFormatWriter().encode(str, BarcodeFormat.QR_CODE, dimension, dimension, null);
        } catch (IllegalArgumentException iae) {
            // Unsupported format
            return null;
        }

        int width = result.getWidth();
        int height = result.getHeight();
        int[] pixels = new int[width * height];
        for (int y = 0; y < height; y++) {
            int offset = y * width;
            for (int x = 0; x < width; x++) {
                pixels[offset + x] = result.get(x, y) ? getResources().getColor(R.color.black) : getResources().getColor(R.color.white);
            }
        }

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return bitmap;
    }
}

