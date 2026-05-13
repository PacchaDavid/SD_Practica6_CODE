package com.fleet.monitor.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleet.monitor.config.RabbitMQConfig;
import com.fleet.monitor.model.GpsMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Actividad 6 – Servicio de Telemetría GPS
 *
 * Consume mensajes de la cola "cola.gps.telemetria", los registra
 * en el log y los almacena en memoria (mapa vehículo → último GPS).
 */
@Component
public class GpsConsumer {

    private static final Logger log = LoggerFactory.getLogger(GpsConsumer.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Almacén en memoria: vehicleId → último GpsMessage recibido
    private final Map<String, GpsMessage> lastGpsData = new ConcurrentHashMap<>();

    // Historial completo (últimos 100 mensajes)
    private final List<GpsMessage> gpsHistory = new ArrayList<>();
    private static final int MAX_HISTORY = 100;

    @RabbitListener(queues = RabbitMQConfig.GPS_QUEUE)
    public void consumeGps(String message) {
        try {
            log.info("📍 GPS recibido: {}", message);

            GpsMessage gps = objectMapper.readValue(message, GpsMessage.class);

            // Guardar último dato por vehículo
            lastGpsData.put(gps.getVehicleId(), gps);

            // Guardar en historial
            synchronized (gpsHistory) {
                if (gpsHistory.size() >= MAX_HISTORY) {
                    gpsHistory.remove(0);
                }
                gpsHistory.add(gps);
            }

            log.info("✅ GPS almacenado – Vehículo: {} | Lat: {} | Lng: {} | Vel: {} km/h",
                    gps.getVehicleId(), gps.getLat(), gps.getLng(), gps.getSpeed());

        } catch (Exception e) {
            log.error("❌ Error procesando GPS: {}", e.getMessage());
        }
    }

    // ── Métodos de acceso para el FleetController ─────────────────────────────
    public Map<String, GpsMessage> getLastGpsData() {
        return lastGpsData;
    }

    public List<GpsMessage> getGpsHistory() {
        return new ArrayList<>(gpsHistory);
    }

    public GpsMessage getLastGpsByVehicle(String vehicleId) {
        return lastGpsData.get(vehicleId);
    }
}
