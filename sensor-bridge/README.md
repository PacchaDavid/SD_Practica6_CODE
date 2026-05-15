# Práctica 5 - Mensajería Distribuida con RabbitMQ y MQTT
**Sistemas Distribuidos · 6to Ciclo · FEIRNNR - Carrera de Computación**

---

## Estructura del proyecto

```
practica5/
├── docker-compose.yml              # Mosquitto + RabbitMQ
├── mosquitto/
│   └── config/
│       └── mosquitto.conf          # Configuración del broker MQTT
├── sensor_simulador.py             # Actividad 2: publica telemetría IoT
├── mqtt_suscriptor.py              # Actividad 3: suscriptor + SQLite
├── mqtt_rabbitmq_bridge.py         # Actividad 4: puente MQTT → RabbitMQ
├── requirements.txt
└── README.md
```

---

## Requisitos previos

- Docker y Docker Compose instalados
- Python 3.8+

---

## Paso 1 — Levantar los brokers

```bash
docker-compose up -d
```

Verifica que Mosquitto esté activo (prueba básica):

```bash
# Terminal 1 - Suscriptor de prueba
docker exec -it mosquitto_broker mosquitto_sub -h localhost -t "test/topic"

# Terminal 2 - Publicador de prueba
docker exec -it mosquitto_broker mosquitto_pub -h localhost -t "test/topic" -m "Hola MQTT"
```

RabbitMQ Management UI: http://localhost:15672 (usuario: `guest`, clave: `guest`)

---

## Paso 2 — Instalar dependencias Python

```bash
pip install -r requirements.txt
```

---

## Paso 3 — Ejecutar los componentes

Abrir **tres terminales** distintas en el directorio del proyecto:

### Terminal A — Suscriptor MQTT + SQLite

```bash
python mqtt_suscriptor.py
```

### Terminal B — Bridge MQTT → RabbitMQ

```bash
python mqtt_rabbitmq_bridge.py
```

### Terminal C — Simulador de sensores IoT

```bash
python sensor_simulador.py
```

---

## Funcionamiento esperado

| Componente | Qué hace |
|---|---|
| `sensor_simulador.py` | Publica cada 5 s datos GPS, temperatura y combustible de VH-001, VH-002 y VH-003 en topics `flota/{id}/{sensor}` |
| `mqtt_suscriptor.py` | Recibe todos los mensajes, muestra alertas en consola y guarda todo en `telemetria.db` (SQLite) |
| `mqtt_rabbitmq_bridge.py` | Reenvía datos GPS a `cola.gps.telemetria`; alertas de temperatura (>4°C) a `cola.alertas.temperatura`; alertas de combustible (<20%) a `cola.combustible.nivel` |

---

## Topic hierarchy

```
flota/
 ├── VH-001/
 │    ├── gps
 │    ├── temperatura
 │    └── combustible
 ├── VH-002/  ...
 └── VH-003/  ...
```

Comodines MQTT:
- `flota/+/gps`   → GPS de cualquier vehículo
- `flota/#`       → todo lo de la flota

---

## Umbrales de alerta

| Sensor | Condición | Cola RabbitMQ |
|---|---|---|
| Temperatura | > 4 °C | `cola.alertas.temperatura` |
| Combustible | < 20 % | `cola.combustible.nivel` |

---

## Base de datos SQLite

El archivo `telemetria.db` se crea automáticamente con tres tablas:

```
gps_data   → vehicle_id, lat, lng, speed, timestamp
temp_data  → vehicle_id, temperature, unit, timestamp
fuel_data  → vehicle_id, fuel_level, unit, timestamp
```

Consulta de ejemplo:

```bash
sqlite3 telemetria.db "SELECT * FROM temp_data WHERE temperature > 4 ORDER BY timestamp DESC LIMIT 10;"
```
