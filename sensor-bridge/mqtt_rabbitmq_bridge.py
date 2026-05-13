"""
Actividad 4: Servicio Puente MQTT → RabbitMQ
Práctica 5 - Sistemas Distribuidos
Se suscribe a todos los topics MQTT de la flota, filtra alertas
(temperatura > 4°C o combustible < 20%) y las publica en las
colas correspondientes de RabbitMQ usando pika.
"""

import json
import os
import time
from datetime import datetime

import paho.mqtt.client as mqtt
import pika

# ── Configuración ────────────────────────────────────────────────────────────
MQTT_BROKER    = os.environ.get("MQTT_BROKER", "localhost")
MQTT_PORT      = int(os.environ.get("MQTT_PORT", "1883"))
RABBITMQ_HOST  = os.environ.get("RABBITMQ_HOST", "localhost")
RABBITMQ_PORT  = int(os.environ.get("RABBITMQ_PORT", "5672"))
RABBITMQ_USER  = os.environ.get("RABBITMQ_USER", "guest")
RABBITMQ_PASS  = os.environ.get("RABBITMQ_PASSWORD", "guest")

# Nombres de colas (durable)
COLA_GPS         = "cola.gps.telemetria"
COLA_TEMPERATURA = "cola.alertas.temperatura"
COLA_COMBUSTIBLE = "cola.combustible.nivel"

# Umbrales
UMBRAL_TEMPERATURA = 4.0   # °C
UMBRAL_COMBUSTIBLE = 20.0  # %


# ── RabbitMQ ─────────────────────────────────────────────────────────────────
def configurar_rabbitmq():
    """Crea la conexión, el canal y declara las colas durables."""
    while True:
        try:
            conexion = pika.BlockingConnection(
                pika.ConnectionParameters(
                    host=RABBITMQ_HOST,
                    port=RABBITMQ_PORT,
                    credentials=pika.PlainCredentials(RABBITMQ_USER, RABBITMQ_PASS),
                )
            )
            break
        except pika.exceptions.AMQPConnectionError as error:
            print(f"Esperando RabbitMQ en {RABBITMQ_HOST}:{RABBITMQ_PORT} ({error})")
            time.sleep(3)

    canal = conexion.channel()

    for cola in (COLA_GPS, COLA_TEMPERATURA, COLA_COMBUSTIBLE):
        canal.queue_declare(queue=cola, durable=True)

    print("Colas RabbitMQ creadas exitosamente")
    return conexion, canal


def publicar_rabbitmq(canal: pika.adapters.blocking_connection.BlockingChannel,
                      cola: str, datos: dict):
    """Publica un mensaje persistente en la cola indicada."""
    canal.basic_publish(
        exchange="",
        routing_key=cola,
        body=json.dumps(datos),
        properties=pika.BasicProperties(delivery_mode=2),  # persistente
    )
    print(f"  → [RabbitMQ:{cola}] {json.dumps(datos)}")


# ── Callback MQTT ─────────────────────────────────────────────────────────────
def al_recibir_mensaje_mqtt(cliente, datos_usuario, mensaje):
    topic = mensaje.topic
    try:
        payload = json.loads(mensaje.payload.decode("utf-8"))
    except (json.JSONDecodeError, UnicodeDecodeError) as e:
        print(f"[ERROR] No se pudo decodificar el mensaje en {topic}: {e}")
        return

    canal = datos_usuario["rabbitmq_channel"]

    if "/gps" in topic:
        publicar_rabbitmq(canal, COLA_GPS, payload)

    elif "/temperatura" in topic:
        temperatura = payload.get("temperature", 0)
        if temperatura > UMBRAL_TEMPERATURA:
            alerta = {
                "type":    "TEMP_ALERT",
                "message": "Temperatura excedida",
                **payload,
            }
            publicar_rabbitmq(canal, COLA_TEMPERATURA, alerta)
            print(f"[⚠ ALERTA TEMPERATURA] {payload.get('vehicle_id')} → {temperatura}°C")

    elif "/combustible" in topic:
        nivel = payload.get("fuel_level", 100)
        if nivel < UMBRAL_COMBUSTIBLE:
            alerta = {
                "type":    "FUEL_LOW",
                "message": "Combustible bajo",
                **payload,
            }
            publicar_rabbitmq(canal, COLA_COMBUSTIBLE, alerta)
            print(f"[⚠ ALERTA COMBUSTIBLE] {payload.get('vehicle_id')} → {nivel}%")


# ── Función principal ────────────────────────────────────────────────────────
def principal():
    conexion_rabbit, canal_rabbit = configurar_rabbitmq()

    cliente = mqtt.Client(
        client_id="mqtt_rabbitmq_bridge",
        userdata={"rabbitmq_channel": canal_rabbit},
    )
    cliente.on_message = al_recibir_mensaje_mqtt

    while True:
        try:
            cliente.connect(MQTT_BROKER, MQTT_PORT, keepalive=60)
            break
        except OSError as error:
            print(f"Esperando broker MQTT en {MQTT_BROKER}:{MQTT_PORT} ({error})")
            time.sleep(3)

    cliente.subscribe("flota/#")  # todos los topics de la flota

    print("Bridge MQTT-RabbitMQ iniciado...")
    try:
        cliente.loop_forever()
    except KeyboardInterrupt:
        print("\nDeteniendo bridge...")
        cliente.disconnect()
        conexion_rabbit.close()


if __name__ == "__main__":
    principal()
