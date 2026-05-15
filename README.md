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
---
Última actualización: 2026-05-13
