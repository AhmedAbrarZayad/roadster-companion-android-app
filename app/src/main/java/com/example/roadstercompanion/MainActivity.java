package com.example.roadstercompanion;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.location.CurrentLocationRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_FOREGROUND_LOCATION = 1;
    private static final int REQUEST_BACKGROUND_LOCATION = 2;

    private FusedLocationProviderClient fusedLocationClient;
    private MaterialButton button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        button = findViewById(R.id.location_button);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        button.setOnClickListener(v -> {
            if (hasForegroundLocationPermission()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !hasBackgroundLocationPermission()) {
                    requestBackgroundLocationPermission();
                } else {
                    getLocation();
                }
            } else {
                requestForegroundLocationPermission();
            }
        });
    }

    private void getLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Location permission not granted", Toast.LENGTH_SHORT).show();
            return;
        }

        // Build a high-accuracy current location request
        CurrentLocationRequest locationRequest = new CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setMaxUpdateAgeMillis(0) // Don't use cached data
                .build();

        CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();

        fusedLocationClient.getCurrentLocation(locationRequest, cancellationTokenSource.getToken())
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        String msg = "Latitude: " + location.getLatitude() + "\nLongitude: " + location.getLongitude();
                        Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(MainActivity.this, "Failed to get current location", Toast.LENGTH_SHORT).show();
                    }
                });
    }


    private boolean hasForegroundLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasBackgroundLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestForegroundLocationPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                REQUEST_FOREGROUND_LOCATION);
    }

    private void requestBackgroundLocationPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                REQUEST_BACKGROUND_LOCATION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_FOREGROUND_LOCATION) {
            boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            if (granted) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    requestBackgroundLocationPermission();
                } else {
                    getLocation();
                }
            } else {
                Toast.makeText(this, "Foreground location permission denied", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_BACKGROUND_LOCATION) {
            boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            if (granted) {
                getLocation();
            } else {
                Toast.makeText(this, "Background location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}