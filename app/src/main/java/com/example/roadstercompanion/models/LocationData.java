package com.example.roadstercompanion.models;

public class LocationData {
    private String userId;
    private double latitude;
    private double longitude;
    private double accuracy;
    private double speed;
    private double bearing;
    private long timestamp;

    public LocationData(String userId, double latitude, double longitude, double accuracy, double speed, double bearing) {
        this.userId = userId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.accuracy = accuracy;
        this.speed = speed;
        this.bearing = bearing;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters
    public String getUserId() { return userId; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public double getAccuracy() { return accuracy; }
    public double getSpeed() { return speed; }
    public double getBearing() { return bearing; }
    public long getTimestamp() { return timestamp; }

    // Setters
    public void setUserId(String userId) { this.userId = userId; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public void setAccuracy(double accuracy) { this.accuracy = accuracy; }
    public void setSpeed(double speed) { this.speed = speed; }
    public void setBearing(double bearing) { this.bearing = bearing; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    @Override
    public String toString() {
        return "LocationData{" +
                "userId='" + userId + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", accuracy=" + accuracy +
                ", speed=" + speed +
                ", bearing=" + bearing +
                ", timestamp=" + timestamp +
                '}';
    }
}
