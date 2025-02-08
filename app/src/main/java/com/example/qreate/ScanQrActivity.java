package com.example.qreate;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import android.Manifest;
import android.content.pm.PackageManager;
import android.media.Image;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.widget.TextView;
import android.widget.Toast;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Reader;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class ScanQrActivity extends AppCompatActivity {

    private PreviewView previewView;
    private TextView descriptionTextView;
    private TextView scanCompletedTextView;
    private ExecutorService cameraExecutor;

    private static final int PERMISSION_REQUEST_CODE = 123;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_qr);
        Objects.requireNonNull(getSupportActionBar()).hide();

        previewView = findViewById(R.id.previewView);
        descriptionTextView = findViewById(R.id.descriptionTextView);
        scanCompletedTextView = findViewById(R.id.scanCompletedTextView);

        // Camera permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                    PERMISSION_REQUEST_CODE);
        } else {
            startCamera();
        }

        cameraExecutor = Executors.newSingleThreadExecutor();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void startCamera()
    {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() ->
        {
            try
            {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            }
            catch (ExecutionException | InterruptedException e)
            {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindPreview(@NonNull ProcessCameraProvider cameraProvider)
    {
        Preview preview = new Preview.Builder()
                .build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setTargetResolution(new Size(640, 480))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(cameraExecutor, new ImageAnalysis.Analyzer() {
            @OptIn(markerClass = ExperimentalGetImage.class)
            @Override
            public void analyze(@NonNull ImageProxy imageProxy) {
                Image mediaImage = imageProxy.getImage();
                if (mediaImage != null) {
                    ByteBuffer buffer = mediaImage.getPlanes()[0].getBuffer();
                    byte[] bytes = new byte[buffer.capacity()];
                    buffer.get(bytes);

                    int width = mediaImage.getWidth();
                    int height = mediaImage.getHeight();

                    // QR Code reader
                    int[] intArray = new int[bytes.length];
                    for (int i = 0; i < bytes.length; i++) {
                        intArray[i] = bytes[i] & 0xFF;
                    }

                    LuminanceSource source = new RGBLuminanceSource(width, height, intArray);
                    BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

                    Reader reader = new MultiFormatReader();
                    Result result;

                    try (imageProxy) {
                        result = reader.decode(bitmap);
                        String decryptedData = decryptData(result.getText());

                        runOnUiThread(() -> {
                            descriptionTextView.setText(decryptedData);
                            scanCompletedTextView.setVisibility(TextView.VISIBLE);
                        });

                    } catch (NotFoundException e) {
                        Log.d("QR_SCAN", "QR Code not found");
                    } catch (ChecksumException e) {
                        Log.e("QR_SCAN", "Checksum error", e);
                    } catch (FormatException e) {
                        Log.e("QR_SCAN", "Format error", e);
                    } catch (Exception e) {
                        Log.e("QR_SCAN", "Decryption error", e);
                    }
                }
            }
        });

        cameraProvider.bindToLifecycle((LifecycleOwner)this, cameraSelector, preview, imageAnalysis);
    }

    private String decryptData(String encryptedData) throws Exception {
        String password = "QReate1234567890"; // 16 characters for AES-128
        SecretKeySpec secretKey = new SecretKeySpec(password.getBytes(StandardCharsets.UTF_8), "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] encryptedBytes = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            encryptedBytes = Base64.getDecoder().decode(encryptedData);
        }
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }
}

