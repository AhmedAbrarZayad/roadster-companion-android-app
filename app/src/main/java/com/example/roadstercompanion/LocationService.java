package com.example.roadstercompanion;


import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.example.roadstercompanion.websocket.LocationSender;

public class LocationService extends Service {

    private static final String TAG = "LocationService";
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationSender locationSender;

    // Your ngrok WebSocket URL - FIXED: Changed from /wss to /ws to match Spring Boot config
    private static final String WEBSOCKET_URL = "wss://8569284e503d.ngrok-free.app/ws";
    private static final String USER_ID = "android_user_001"; // You can make this dynamic

    public static final String ACTION_START = "START";
    public static final String ACTION_STOP = "STOP";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Initialize WebSocket connection
        locationSender = new LocationSender(WEBSOCKET_URL);
        locationSender.connectWebSocket();

        locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
                .setMinUpdateIntervalMillis(5000)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;
                for (Location location : locationResult.getLocations()) {
                    Log.d(TAG, "📍 New location received: " + location.getLatitude() + ", " + location.getLongitude());

                    // Send location data via WebSocket to Spring Boot server
                    locationSender.sendLocation(
                        USER_ID,
                        location.getLatitude(),
                        location.getLongitude(),
                        location.getAccuracy(),
                        location.hasSpeed() ? location.getSpeed() : 0.0,
                        location.hasBearing() ? location.getBearing() : 0.0
                    );

                    // Show toast for debugging (you can remove this later)
                    Toast.makeText(LocationService.this,
                            "📡 Location sent: " + location.getLatitude() + ", " + location.getLongitude(),
                            Toast.LENGTH_SHORT).show();
                }
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopLocationUpdates();
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }

        createNotificationChannel();
        startLocationUpdates();
        return START_STICKY;
    }

    private void createNotificationChannel() {
        String channelId = "location_channel";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Location Tracking",
                    NotificationManager.IMPORTANCE_DEFAULT  // Changed from LOW
            );
            channel.setDescription("Tracks your location in the background");
            channel.enableLights(true);
            channel.enableVibration(false);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, channelId)
                .setContentTitle("Location Tracking Active")
                .setContentText("Tracking your location")
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)  // Changed from LOW
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .build();

        startForeground(1, notification);
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());

        } else {
            Toast.makeText(this, "Permissions not granted", Toast.LENGTH_SHORT).show();
            stopSelf(); // Stop service if no permissions
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (fusedLocationClient != null && locationCallback != null) {
            stopLocationUpdates();
        }
        // Close WebSocket connection
        if (locationSender != null) {
            locationSender.disconnect();
        }
    }
    private void stopLocationUpdates() {
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }
}