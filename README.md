# Fleet Monitor — Proyecto de práctica

Este repositorio contiene una pila completa de ejemplo para monitorizar una flota: simulador de sensores MQTT, puente MQTT→RabbitMQ, microservicio Java (Spring Boot) que consume mensajes y expone una API REST, y un dashboard web servido por un pequeño servidor Python.

Requisitos
- Docker y Docker Compose (v2) instalados en el sistema
- Puertos libres: 1883 (MQTT), 9001 (MQTT WebSocket), 5672 (RabbitMQ), 15672 (RabbitMQ management), 8080 (API), 3000 (Dashboard)

Credenciales por defecto
- RabbitMQ: usuario `admin`, contraseña `admin123`

Arrancar todo con Docker Compose
1. En la raíz del repo ejecutar:

```bash
docker compose up --build -d
```

2. Espera a que los servicios inicien (puede tardar unos segundos).

Acceder a los servicios
- Dashboard web: http://localhost:3000
- API Java (endpoints REST): http://localhost:8080/api/fleet
- RabbitMQ Management: http://localhost:15672 (credenciales arriba)

Comandos útiles
- Ver logs de un servicio:

```bash
docker compose logs -f dashboard
docker compose logs -f fleet-api
```

- Reconstruir sólo el dashboard (frontend):

```bash
docker compose build dashboard
docker compose up -d dashboard
```

- Parar y eliminar contenedores:

```bash
docker compose down
```

Problemas comunes y soluciones rápidas
- Error de conexión en los servicios Python al arrancar: los brokers pueden tardar en estar listos; los servicios incluyen reintentos automáticos — vuelve a probar `docker compose up`.
- El mapa en tiempo real usa MQTT WebSocket en el puerto `9001`. Si el mapa no muestra vehículos, asegúrate de que `mosquitto` esté publicado en el puerto 9001 y que el dashboard esté configurado con `ws://<host>:9001/mqtt` en el campo MQTT WebSocket.
- Si el microservicio Java lanza excepciones de deserialización AMQP, revisa que RabbitMQ esté corriendo y que el puente MQTT→RabbitMQ esté enviando mensajes JSON.

Desarrollo local sin Docker (opcional)
- Backend Java: desde `fleet-monitor/` ejecutar `mvn -DskipTests package` y luego `java -jar target/*.jar` (requiere Java 17).
- Dashboard: desde `frontend/` ejecutar `python3 server.py` y abrir `http://localhost:3000`.

Estructura principal
- `fleet-monitor/` — microservicio Java (Spring Boot)
- `sensor-bridge/` — scripts Python: simulador MQTT, puente mqtt->rabbitmq, suscriptor
- `frontend/` — HTML/JS del dashboard y `server.py` para servirlo
- `docker-compose.yml` — orquestración de todos los servicios

Si necesitas que actualice el README para incluir más detalles (endpoints específicos, diagramas, cómo ejecutar pruebas unitarias, credenciales diferentes), dímelo y lo amplío.

---
Última actualización: 2026-05-13
