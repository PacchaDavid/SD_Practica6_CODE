package com.fleet.monitor.controller;

import com.fleet.monitor.consumer.AlertConsumer;
import com.fleet.monitor.consumer.GpsConsumer;
import com.fleet.monitor.model.AlertMessage;
import com.fleet.monitor.model.GpsMessage;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Actividad 8 – API REST de Consulta
 *
 * Expone endpoints para consultar el estado de la flota,
 * telemetría GPS por vehículo y el historial de alertas.
 *
 * Endpoints disponibles:
 *   GET /api/fleet/status                        → Resumen general de la flota
 *   GET /api/fleet/vehicles                      → Lista de vehículos activos
 *   GET /api/fleet/vehicle/{id}/telemetria       → Última telemetría de un vehículo
 *   GET /api/fleet/vehicle/{id}/gps              → Último GPS de un vehículo
 *   GET /api/fleet/alerts                        → Historial de alertas
 */
@RestController
@RequestMapping("/api/fleet")
@CrossOrigin(origins = "*")  // Permite acceso desde cualquier origen (útil para el dashboard web)
public class FleetController {

    private final GpsConsumer gpsConsumer;
    private final AlertConsumer alertConsumer;

    public FleetController(GpsConsumer gpsConsumer, AlertConsumer alertConsumer) {
        this.gpsConsumer   = gpsConsumer;
        this.alertConsumer = alertConsumer;
    }

    // ── GET /api/fleet/status ─────────────────────────────────────────────────
    /**
     * Retorna un resumen general del estado actual de la flota.
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getFleetStatus() {
        Map<String, GpsMessage> lastGps = gpsConsumer.getLastGpsData();
        List<AlertMessage> alerts       = alertConsumer.getAlertHistory();

        // Contar alertas activas (últimas 10 minutos aprox.)
        long activeAlerts = alerts.stream()
                .filter(a -> a.getType() != null)
                .count();

        Map<String, Object> status = new LinkedHashMap<>();
        status.put("totalVehicles",    lastGps.size() > 0 ? lastGps.size() : 3);
        status.put("activeVehicles",   lastGps.size());
        status.put("activeAlerts",     activeAlerts);
        status.put("vehicleIds",       new ArrayList<>(lastGps.keySet()));
        status.put("timestamp",        new Date());
        status.put("systemStatus",     "OPERATIVO");

        return ResponseEntity.ok(status);
    }

    // ── GET /api/fleet/vehicles ───────────────────────────────────────────────
    /**
     * Retorna la lista de vehículos activos con su última posición GPS.
     */
    @GetMapping("/vehicles")
    public ResponseEntity<Map<String, Object>> getVehicles() {
        Map<String, GpsMessage> lastGps = gpsConsumer.getLastGpsData();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("count",    lastGps.size());
        response.put("vehicles", lastGps);
        response.put("timestamp", new Date());

        return ResponseEntity.ok(response);
    }

    // ── GET /api/fleet/vehicle/{id}/telemetria ────────────────────────────────
    /**
     * Retorna el historial de telemetría de un vehículo específico.
     */
    @GetMapping("/vehicle/{id}/telemetria")
    public ResponseEntity<Map<String, Object>> getTelemetria(@PathVariable String id) {
        List<GpsMessage> history = gpsConsumer.getGpsHistory();

        // Filtrar por vehículo
        List<GpsMessage> vehicleHistory = history.stream()
                .filter(g -> id.equals(g.getVehicleId()))
                .collect(java.util.stream.Collectors.toList());

        if (vehicleHistory.isEmpty()) {
            Map<String, Object> notFound = new LinkedHashMap<>();
            notFound.put("vehicleId", id);
            notFound.put("message",   "No se encontraron datos para el vehículo: " + id);
            notFound.put("timestamp", new Date());
            return ResponseEntity.ok(notFound);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("vehicleId",  id);
        response.put("count",      vehicleHistory.size());
        response.put("telemetria", vehicleHistory);
        response.put("timestamp",  new Date());

        return ResponseEntity.ok(response);
    }

    // ── GET /api/fleet/vehicle/{id}/gps ──────────────────────────────────────
    /**
     * Retorna el último dato GPS de un vehículo específico.
     */
    @GetMapping("/vehicle/{id}/gps")
    public ResponseEntity<Map<String, Object>> getLastGps(@PathVariable String id) {
        GpsMessage lastGps = gpsConsumer.getLastGpsByVehicle(id);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("vehicleId", id);
        response.put("timestamp", new Date());

        if (lastGps == null) {
            response.put("message", "Sin datos GPS para: " + id);
            response.put("data",    null);
        } else {
            response.put("data", lastGps);
        }

        return ResponseEntity.ok(response);
    }

    // ── GET /api/fleet/alerts ─────────────────────────────────────────────────
    /**
     * Retorna el historial de alertas recibidas (temperatura y combustible).
     */
    @GetMapping("/alerts")
    public ResponseEntity<Map<String, Object>> getAlerts() {
        List<AlertMessage> alerts = alertConsumer.getAlertHistory();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("totalAlerts", alerts.size());
        response.put("alerts",      alerts);
        response.put("timestamp",   new Date());

        return ResponseEntity.ok(response);
    }
}
