package com.example.genai_hackathon;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.util.Log;

import androidx.annotation.*;
import android.Manifest;
import android.content.pm.PackageManager;
import android.content.Intent;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.view.View;
import android.location.Location;
import android.location.Geocoder;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.widget.Button;
import android.widget.Toast;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.FusedLocationProviderClient;

import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    public static final String MY_TAG = "custom_log";
    // instances
    private FusedLocationProviderClient fusedLocationClient;
    // permission code
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 2;
    private static final int MICROPHONE_PERMISSION_REQUEST_CODE = 3;
    private boolean locationPermissionGranted = false;
    private boolean cameraPermissionGranted = false;
    private boolean recordPermissionGranted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        Log.i(MY_TAG, "onCreate");
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // location client initialized
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        // start button
        Button startButton = findViewById(R.id.startButton);
        startButton.setOnClickListener((View v) -> {
                requestLocationPermissions();
                requestCameraPermission();
                requestMicrophonePermission();

                // pass permissions to the next activity
                Intent intent = new Intent(MainActivity.this, InteractiveActivity.class);
                intent.putExtra("RECORD_PERMISSION_GRANTED", recordPermissionGranted);
                startActivity(intent);
            }
        );
    }

    private void requestLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            }, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            getLocationInfo();
        }
    }

    private void requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST_CODE);
        } else {
            getCameraInfo();
        }
    }

    private void requestMicrophonePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO},
                    MICROPHONE_PERMISSION_REQUEST_CODE);
        } else {
            getMicrophoneInfo();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            switch (requestCode) {
                case LOCATION_PERMISSION_REQUEST_CODE:
                    locationPermissionGranted = true;
                    getLocationInfo();
                    break;
                case CAMERA_PERMISSION_REQUEST_CODE:
                    cameraPermissionGranted = true;
                    getCameraInfo();
                    break;
                case MICROPHONE_PERMISSION_REQUEST_CODE:
                    recordPermissionGranted = true;
                    getMicrophoneInfo();
                    break;
            }
        } else {
            Toast.makeText(this, "permission denied", Toast.LENGTH_SHORT).show();
        }
    }

    private void getLocationInfo() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        double latitude = location.getLatitude();
                        double longitude = location.getLongitude();
                        getAddressFromLocation(latitude, longitude);
                    } else {
                        Toast.makeText(this, "无法获取位置", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void getAddressFromLocation(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<android.location.Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                String address = addresses.get(0).getAddressLine(0); // 获取完整地址
                Toast.makeText(this, "Place: " + address, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Address not found", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Failed obtaining address: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void getCameraInfo() {

    }
    private void getMicrophoneInfo() {

    }
}