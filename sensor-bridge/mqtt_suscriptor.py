"""
Actividad 3: Suscriptor MQTT y Almacenamiento en SQLite
Práctica 5 - Sistemas Distribuidos
Escucha los topics de la flota, genera alertas automáticas y
almacena la telemetría en una base de datos SQLite.
"""

import json
import sqlite3
import os
import time
from datetime import datetime
import paho.mqtt.client as mqtt

# ── Configuración MQTT ──────────────────────────────────────────────────────
BROKER  = os.environ.get("MQTT_BROKER", "localhost")
PUERTO  = int(os.environ.get("MQTT_PORT", "1883"))
DB_PATH = os.environ.get("DB_PATH", "telemetria.db")

# Umbrales de alerta
UMBRAL_TEMPERATURA = 4.0   # °C
UMBRAL_COMBUSTIBLE = 20.0  # %


# ── Base de datos ────────────────────────────────────────────────────────────
def inicializar_base_datos():
    conn   = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()

    cursor.execute("""
        CREATE TABLE IF NOT EXISTS gps_data (
            id         INTEGER PRIMARY KEY AUTOINCREMENT,
            vehicle_id TEXT    NOT NULL,
            lat        REAL    NOT NULL,
            lng        REAL    NOT NULL,
            speed      REAL    NOT NULL,
            timestamp  TEXT    NOT NULL
        )
    """)

    cursor.execute("""
        CREATE TABLE IF NOT EXISTS temp_data (
            id          INTEGER PRIMARY KEY AUTOINCREMENT,
            vehicle_id  TEXT NOT NULL,
            temperature REAL NOT NULL,
            unit        TEXT NOT NULL,
            timestamp   TEXT NOT NULL
        )
    """)

    cursor.execute("""
        CREATE TABLE IF NOT EXISTS fuel_data (
            id         INTEGER PRIMARY KEY AUTOINCREMENT,
            vehicle_id TEXT NOT NULL,
            fuel_level REAL NOT NULL,
            unit       TEXT NOT NULL,
            timestamp  TEXT NOT NULL
        )
    """)

    conn.commit()
    conn.close()
    print("Base de datos inicializada.")


# ── Callback de mensajes ─────────────────────────────────────────────────────
def al_recibir_mensaje(cliente, datos_usuario, mensaje):
    topic   = mensaje.topic
    try:
        payload = json.loads(mensaje.payload.decode("utf-8"))
    except (json.JSONDecodeError, UnicodeDecodeError) as e:
        print(f"[ERROR] No se pudo decodificar el mensaje en {topic}: {e}")
        return

    conn   = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()

    if "/gps" in topic:
        cursor.execute(
            "INSERT INTO gps_data (vehicle_id, lat, lng, speed, timestamp) VALUES (?, ?, ?, ?, ?)",
            (payload["vehicle_id"], payload["lat"], payload["lng"],
             payload["speed"], payload["timestamp"])
        )
        print(f"[GPS]  {payload['vehicle_id']} | lat={payload['lat']} lng={payload['lng']} speed={payload['speed']} km/h")

    elif "/temperatura" in topic:
        temperatura = payload["temperature"]
        if temperatura > UMBRAL_TEMPERATURA:
            print(f"[⚠ ALERTA TEMPERATURA] {payload['vehicle_id']} → {temperatura}°C (umbral: {UMBRAL_TEMPERATURA}°C)")

        cursor.execute(
            "INSERT INTO temp_data (vehicle_id, temperature, unit, timestamp) VALUES (?, ?, ?, ?)",
            (payload["vehicle_id"], temperatura, payload["unit"], payload["timestamp"])
        )
        print(f"[TEMP] {payload['vehicle_id']} | {temperatura}{payload['unit']}")

    elif "/combustible" in topic:
        nivel = payload["fuel_level"]
        if nivel < UMBRAL_COMBUSTIBLE:
            print(f"[⚠ ALERTA COMBUSTIBLE] {payload['vehicle_id']} → {nivel}% (umbral: {UMBRAL_COMBUSTIBLE}%)")

        cursor.execute(
            "INSERT INTO fuel_data (vehicle_id, fuel_level, unit, timestamp) VALUES (?, ?, ?, ?)",
            (payload["vehicle_id"], nivel, payload["unit"], payload["timestamp"])
        )
        print(f"[FUEL] {payload['vehicle_id']} | {nivel}{payload['unit']}")

    conn.commit()
    conn.close()


# ── Función principal ────────────────────────────────────────────────────────
def principal():
    inicializar_base_datos()

    cliente = mqtt.Client(client_id="mqtt_suscriptor")
    cliente.on_message = al_recibir_mensaje

    while True:
        try:
            cliente.connect(BROKER, PUERTO, keepalive=60)
            break
        except OSError as error:
            print(f"Esperando broker MQTT en {BROKER}:{PUERTO} ({error})")
            time.sleep(3)

    cliente.subscribe("flota/+/gps")
    cliente.subscribe("flota/+/temperatura")
    cliente.subscribe("flota/+/combustible")

    print("Suscriptor activo. Escuchando topics flota/+/...")
    try:
        cliente.loop_forever()
    except KeyboardInterrupt:
        print("\nDeteniendo suscriptor...")
        cliente.disconnect()


if __name__ == "__main__":
    principal()
