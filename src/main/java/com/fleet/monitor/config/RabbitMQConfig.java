package com.fleet.monitor.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Nombres de las colas
    public static final String GPS_QUEUE            = "cola.gps.telemetria";
    public static final String TEMP_ALERT_QUEUE     = "cola.alertas.temperatura";
    public static final String FUEL_QUEUE           = "cola.combustible.nivel";
    public static final String NOTIFICATION_QUEUE   = "cola.notificaciones";

    // Nombre del exchange
    public static final String FLEET_EXCHANGE       = "exchange.fleet";

    // ── Exchange ──────────────────────────────────────────────────────────────
    @Bean
    public DirectExchange fleetExchange() {
        return new DirectExchange(FLEET_EXCHANGE);
    }

    // ── Colas (durable = true para sobrevivir reinicios) ─────────────────────
    @Bean
    public Queue gpsQueue() {
        return new Queue(GPS_QUEUE, true);
    }

    @Bean
    public Queue tempAlertQueue() {
        return new Queue(TEMP_ALERT_QUEUE, true);
    }

    @Bean
    public Queue fuelQueue() {
        return new Queue(FUEL_QUEUE, true);
    }

    @Bean
    public Queue notificationQueue() {
        return new Queue(NOTIFICATION_QUEUE, true);
    }

    // ── Bindings (cola ↔ exchange mediante routing key) ───────────────────────
    @Bean
    public Binding gpsBinding(Queue gpsQueue, DirectExchange fleetExchange) {
        return BindingBuilder.bind(gpsQueue)
                .to(fleetExchange).with("gps.routing");
    }

    @Bean
    public Binding tempBinding(Queue tempAlertQueue, DirectExchange fleetExchange) {
        return BindingBuilder.bind(tempAlertQueue)
                .to(fleetExchange).with("temp.alert");
    }

    @Bean
    public Binding fuelBinding(Queue fuelQueue, DirectExchange fleetExchange) {
        return BindingBuilder.bind(fuelQueue)
                .to(fleetExchange).with("fuel.routing");
    }

    // ── Conversor JSON para serializar/deserializar mensajes ──────────────────
    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}
