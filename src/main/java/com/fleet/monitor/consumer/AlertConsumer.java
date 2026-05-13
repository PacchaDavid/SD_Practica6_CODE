package com.fleet.monitor.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleet.monitor.config.RabbitMQConfig;
import com.fleet.monitor.model.AlertMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Actividad 7 – Servicio de Alertas
 *
 * Consume mensajes de las colas de temperatura y combustible,
 * genera una notificación enriquecida y la reenvía a
 * "cola.notificaciones" (patrón routing/redistribución).
 */
@Component
public class AlertConsumer {

    private static final Logger log = LoggerFactory.getLogger(AlertConsumer.class);
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Historial de alertas en memoria
    private final List<AlertMessage> alertHistory = new ArrayList<>();
    private static final int MAX_ALERTS = 50;

    public AlertConsumer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    // ── Consumidor de alertas de TEMPERATURA ─────────────────────────────────
    @RabbitListener(queues = RabbitMQConfig.TEMP_ALERT_QUEUE)
    public void consumeTempAlert(String message) {
        try {
            log.warn("🌡️  ALERTA TEMPERATURA recibida: {}", message);

            AlertMessage alert = objectMapper.readValue(message, AlertMessage.class);
            alert.setType("TEMP_ALERT");

            // Enriquecer con nivel de severidad
            String severity = alert.getValue() > 8 ? "CRÍTICO" : "ADVERTENCIA";
            log.warn("⚠️  [{}] Vehículo: {} | Temperatura: {}°C",
                    severity, alert.getVehicleId(), alert.getValue());

            // Reenviar a cola de notificaciones
            rabbitTemplate.convertAndSend(RabbitMQConfig.NOTIFICATION_QUEUE, message);
            log.info("📨 Alerta de temperatura reenviada a cola.notificaciones");

            guardarAlerta(alert);

        } catch (Exception e) {
            log.error("❌ Error procesando alerta de temperatura: {}", e.getMessage());
        }
    }

    // ── Consumidor de alertas de COMBUSTIBLE ─────────────────────────────────
    @RabbitListener(queues = RabbitMQConfig.FUEL_QUEUE)
    public void consumeFuelAlert(String message) {
        try {
            log.warn("⛽ ALERTA COMBUSTIBLE recibida: {}", message);

            AlertMessage alert = objectMapper.readValue(message, AlertMessage.class);
            alert.setType("FUEL_LOW");

            String severity = alert.getValue() < 10 ? "CRÍTICO" : "BAJO";
            log.warn("⚠️  [{}] Vehículo: {} | Combustible: {}%%",
                    severity, alert.getVehicleId(), alert.getValue());

            // Reenviar a cola de notificaciones
            rabbitTemplate.convertAndSend(RabbitMQConfig.NOTIFICATION_QUEUE, message);
            log.info("📨 Alerta de combustible reenviada a cola.notificaciones");

            guardarAlerta(alert);

        } catch (Exception e) {
            log.error("❌ Error procesando alerta de combustible: {}", e.getMessage());
        }
    }

    // ── Consumidor de NOTIFICACIONES (cola centralizada) ─────────────────────
    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_QUEUE)
    public void consumeNotification(String message) {
        try {
            log.info("🔔 NOTIFICACIÓN procesada: {}", message);
            // Aquí se podría integrar envío de email/SMS
        } catch (Exception e) {
            log.error("❌ Error procesando notificación: {}", e.getMessage());
        }
    }

    // ── Utilidad interna ──────────────────────────────────────────────────────
    private void guardarAlerta(AlertMessage alert) {
        synchronized (alertHistory) {
            if (alertHistory.size() >= MAX_ALERTS) {
                alertHistory.remove(0);
            }
            alertHistory.add(alert);
        }
    }

    public List<AlertMessage> getAlertHistory() {
        return new ArrayList<>(alertHistory);
    }
}
