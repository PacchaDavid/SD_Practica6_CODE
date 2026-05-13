"""
Actividad 2: Simulador de Sensores IoT
Práctica 5 - Sistemas Distribuidos
Simula sensores GPS, temperatura y combustible de 3 vehículos
y los publica en topics MQTT jerárquicos.
"""

import json
import time
import random
import math
import os
from datetime import datetime
import paho.mqtt.client as mqtt

# ── Configuración MQTT ──────────────────────────────────────────────────────
BROKER    = os.environ.get("MQTT_BROKER", "localhost")
PUERTO    = int(os.environ.get("MQTT_PORT", "1883"))
VEHICULOS = ["VH-001", "VH-002", "VH-003"]

# Coordenadas base: Guayaquil, Ecuador
BASE_LAT = -2.1709
BASE_LNG = -79.9224


# ── Callbacks ───────────────────────────────────────────────────────────────
def on_connect(cliente, datos_usuario, flags, codigo_respuesta):
    if codigo_respuesta == 0:
        print(f"[{datetime.now().isoformat()}] Conectado al broker MQTT (rc={codigo_respuesta})")
    else:
        print(f"[{datetime.now().isoformat()}] Error al conectar al broker MQTT (rc={codigo_respuesta})")


# ── Funciones de simulación ──────────────────────────────────────────────────
def simular_gps(id_vehiculo: str) -> dict:
    latitud   = BASE_LAT + random.uniform(-0.01, 0.01)
    longitud  = BASE_LNG + random.uniform(-0.01, 0.01)
    velocidad = random.uniform(20, 80)
    return {
        "lat":   round(latitud,   6),
        "lng":   round(longitud,  6),
        "speed": round(velocidad, 1),
    }


def simular_temperatura(id_vehiculo: str) -> dict:
    temperatura = random.uniform(-5, 8)
    return {
        "temperature": round(temperatura, 1),
        "unit":        "celsius",
    }


def simular_combustible(id_vehiculo: str) -> dict:
    combustible = random.uniform(10, 100)
    return {
        "fuel_level": round(combustible, 1),
        "unit":       "percent",
    }


# ── Publicar en MQTT ─────────────────────────────────────────────────────────
def publicar(cliente: mqtt.Client, topic: str, datos: dict):
    payload = json.dumps(datos)
    cliente.publish(topic, payload, qos=1)
    print(f"  → [{topic}] {payload}")


# ── Función principal ────────────────────────────────────────────────────────
def principal():
    cliente = mqtt.Client(client_id="sensor_simulador")
    cliente.on_connect = on_connect

    while True:
        try:
            cliente.connect(BROKER, PUERTO, keepalive=60)
            break
        except OSError as error:
            print(f"[{datetime.now().isoformat()}] Esperando broker MQTT en {BROKER}:{PUERTO} ({error})")
            time.sleep(3)

    cliente.loop_start()

    # Esperar conexión
    time.sleep(1)

    print("Iniciando simulador de sensores IoT...")
    try:
        while True:
            for vehiculo in VEHICULOS:
                timestamp = datetime.now().isoformat()

                # ── GPS ──────────────────────────────────────────────────────
                gps       = simular_gps(vehiculo)
                datos_gps = {"vehicle_id": vehiculo, "timestamp": timestamp, **gps}
                publicar(cliente, f"flota/{vehiculo}/gps", datos_gps)

                # ── Temperatura ──────────────────────────────────────────────
                temp       = simular_temperatura(vehiculo)
                datos_temp = {"vehicle_id": vehiculo, "timestamp": timestamp, **temp}
                publicar(cliente, f"flota/{vehiculo}/temperatura", datos_temp)

                # ── Combustible ──────────────────────────────────────────────
                fuel       = simular_combustible(vehiculo)
                datos_fuel = {"vehicle_id": vehiculo, "timestamp": timestamp, **fuel}
                publicar(cliente, f"flota/{vehiculo}/combustible", datos_fuel)

                print(f"[{timestamp}] Datos publicados para vehículo {vehiculo}")

            time.sleep(5)

    except KeyboardInterrupt:
        print("\nDeteniendo simulador...")
        cliente.loop_stop()
        cliente.disconnect()


if __name__ == "__main__":
    principal()
