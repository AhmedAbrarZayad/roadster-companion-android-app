package com.example.roadstercompanion.websocket;

import android.util.Log;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.java_websocket.drafts.Draft_6455;
import com.google.gson.Gson;
import com.example.roadstercompanion.models.LocationData;
import java.net.URI;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class LocationSender {
    private static final String TAG = "LocationSender";
    private WebSocketClient webSocketClient;
    private Gson gson = new Gson();
    private String serverUrl;
    private boolean isConnected = false;
    private boolean isConnecting = false;
    private ScheduledExecutorService reconnectScheduler;
    private int reconnectAttempts = 0;
    private static final int MAX_RECONNECT_ATTEMPTS = 5;

    public LocationSender(String serverUrl) {
        this.serverUrl = serverUrl;
        this.reconnectScheduler = Executors.newSingleThreadScheduledExecutor();
    }

    public void connectWebSocket() {
        if (isConnecting || isConnected) {
            Log.d(TAG, "Already connecting or connected");
            return;
        }

        isConnecting = true;

        try {
            URI uri = URI.create(serverUrl);
            Log.d(TAG, "🔄 Attempting to connect to: " + serverUrl);

            webSocketClient = new WebSocketClient(uri, new Draft_6455()) {
                @Override
                public void onOpen(ServerHandshake serverHandshake) {
                    Log.d(TAG, "✅ Connected to WebSocket server");
                    isConnected = true;
                    isConnecting = false;
                    reconnectAttempts = 0;

                    // Send STOMP CONNECT frame
                    String connectFrame = "CONNECT\n" +
                            "accept-version:1.1,1.0\n" +
                            "heart-beat:10000,10000\n\n\u0000";
                    send(connectFrame);
                    Log.d(TAG, "📤 Sent CONNECT frame");
                }

                @Override
                public void onMessage(String message) {
                    Log.d(TAG, "📨 Received from server: " + message);

                    // Handle STOMP CONNECTED frame
                    if (message.startsWith("CONNECTED")) {
                        Log.d(TAG, "✅ STOMP connection established");
                        // Subscribe to location updates topic
                        String subscribeFrame = "SUBSCRIBE\n" +
                                "id:sub-0\n" +
                                "destination:/topic/locations\n\n\u0000";
                        send(subscribeFrame);
                    }
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    Log.d(TAG, "🔴 WebSocket connection closed: " + reason + " (Code: " + code + ")");
                    isConnected = false;
                    isConnecting = false;

                    // Schedule reconnection if not manually disconnected
                    if (code != 1000 && reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
                        scheduleReconnect();
                    }
                }

                @Override
                public void onError(Exception ex) {
                    Log.e(TAG, "❌ WebSocket error: " + ex.getMessage(), ex);
                    isConnected = false;
                    isConnecting = false;

                    // Schedule reconnection
                    if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
                        scheduleReconnect();
                    }
                }
            };

            webSocketClient.connect();

        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to create WebSocket connection: " + e.getMessage(), e);
            isConnecting = false;
            scheduleReconnect();
        }
    }

    private void scheduleReconnect() {
        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            Log.e(TAG, "❌ Max reconnection attempts reached. Giving up.");
            return;
        }

        reconnectAttempts++;
        long delay = Math.min(30, 5 * reconnectAttempts);

        Log.d(TAG, "🔄 Scheduling reconnection attempt " + reconnectAttempts + " in " + delay + " seconds");

        reconnectScheduler.schedule(() -> {
            Log.d(TAG, "🔄 Reconnection attempt " + reconnectAttempts);
            connectWebSocket();
        }, delay, TimeUnit.SECONDS);
    }

    public void sendLocation(String userId, double lat, double lng, double accuracy, double speed, double bearing) {
        if (webSocketClient != null && webSocketClient.isOpen() && isConnected) {
            try {
                LocationData locationData = new LocationData(userId, lat, lng, accuracy, speed, bearing);
                String json = gson.toJson(locationData);

                // Send STOMP message to /app/location endpoint (matches Spring Boot controller)
                String stompMessage = "SEND\n" +
                        "destination:/app/location\n" +
                        "content-type:application/json\n\n" +
                        json + "\u0000";

                webSocketClient.send(stompMessage);
                Log.d(TAG, "📍 Sent location data: " + json);

            } catch (Exception e) {
                Log.e(TAG, "❌ Failed to send location: " + e.getMessage(), e);
            }
        } else {
            Log.w(TAG, "⚠️ WebSocket not connected, cannot send location");
            // Try to reconnect
            if (!isConnecting && !isConnected) {
                connectWebSocket();
            }
        }
    }

    public void disconnect() {
        reconnectAttempts = MAX_RECONNECT_ATTEMPTS;

        if (webSocketClient != null && !webSocketClient.isClosed()) {
            String disconnectFrame = "DISCONNECT\n\n\u0000";
            webSocketClient.send(disconnectFrame);
            webSocketClient.close();
            Log.d(TAG, "🔌 Disconnected from WebSocket server");
        }
        isConnected = false;
        isConnecting = false;

        if (reconnectScheduler != null) {
            reconnectScheduler.shutdown();
        }
    }

    public boolean isConnected() {
        return isConnected && webSocketClient != null && webSocketClient.isOpen();
    }
}