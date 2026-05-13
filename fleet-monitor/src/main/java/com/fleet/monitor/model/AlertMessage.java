package com.fleet.monitor.model;

/**
 * Modelo que representa una alerta generada por el puente MQTT-RabbitMQ.
 * Puede ser de tipo TEMP_ALERT (temperatura) o FUEL_LOW (combustible).
 */
public class AlertMessage {

    private String type;        // "TEMP_ALERT" | "FUEL_LOW"
    private String message;     // Descripción de la alerta
    private String vehicleId;
    private String timestamp;
    private double value;       // Temperatura o nivel de combustible

    // ── Constructores ─────────────────────────────────────────────────────────
    public AlertMessage() {}

    // ── Getters y Setters ─────────────────────────────────────────────────────
    public String getType()                     { return type; }
    public void   setType(String type)          { this.type = type; }

    public String getMessage()                  { return message; }
    public void   setMessage(String message)    { this.message = message; }

    public String getVehicleId()                { return vehicleId; }
    public void   setVehicleId(String v)        { this.vehicleId = v; }

    public String getTimestamp()                { return timestamp; }
    public void   setTimestamp(String t)        { this.timestamp = t; }

    public double getValue()                    { return value; }
    public void   setValue(double value)        { this.value = value; }

    @Override
    public String toString() {
        return "AlertMessage{type='" + type + "', vehicleId='" + vehicleId +
               "', value=" + value + ", message='" + message + "'}";
    }
}
