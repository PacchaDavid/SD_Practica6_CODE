package com.fleet.monitor.model;

/**
 * Modelo que representa un mensaje de telemetría GPS
 * publicado por los sensores de los vehículos.
 */
public class GpsMessage {

    private String vehicleId;
    private String timestamp;
    private double lat;
    private double lng;
    private double speed;

    // ── Constructores ─────────────────────────────────────────────────────────
    public GpsMessage() {}

    public GpsMessage(String vehicleId, String timestamp,
                      double lat, double lng, double speed) {
        this.vehicleId = vehicleId;
        this.timestamp = timestamp;
        this.lat       = lat;
        this.lng       = lng;
        this.speed     = speed;
    }

    // ── Getters y Setters ─────────────────────────────────────────────────────
    public String getVehicleId()                    { return vehicleId; }
    public void   setVehicleId(String vehicleId)    { this.vehicleId = vehicleId; }

    public String getTimestamp()                    { return timestamp; }
    public void   setTimestamp(String timestamp)    { this.timestamp = timestamp; }

    public double getLat()                          { return lat; }
    public void   setLat(double lat)                { this.lat = lat; }

    public double getLng()                          { return lng; }
    public void   setLng(double lng)                { this.lng = lng; }

    public double getSpeed()                        { return speed; }
    public void   setSpeed(double speed)            { this.speed = speed; }

    @Override
    public String toString() {
        return "GpsMessage{vehicleId='" + vehicleId + "', lat=" + lat +
               ", lng=" + lng + ", speed=" + speed + ", timestamp='" + timestamp + "'}";
    }
}
