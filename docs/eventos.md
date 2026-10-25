# Contrato de eventos (Hito 3)

Acuerdo común para la comunicación asincrónica con RabbitMQ. Lo siguen todos los servicios que publican o consumen eventos. gRPC sigue siendo el medio para las operaciones que necesitan respuesta inmediata.

## Broker

| Dato | Valor |
|---|---|
| Broker | RabbitMQ (imagen `rabbitmq:3-management`) |
| Host y puerto | `localhost:5672` |
| Consola web | `http://localhost:15672` |
| Usuario | variable de entorno `RABBITMQ_USER` (por defecto `guest`) |
| Contraseña | variable de entorno `RABBITMQ_PASSWORD` (por defecto `guest`) |
| Exchange | `rentar.eventos`, tipo `topic`, durable |
| Routing key | el nombre del evento, por ejemplo `reserva.confirmada` |

Propiedades de Spring para los servicios Java:

```properties
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=${RABBITMQ_USER:guest}
spring.rabbitmq.password=${RABBITMQ_PASSWORD:guest}
```

Cada consumidor tiene su propia cola durable, con el nombre `<servicio>.<evento>`, enlazada al exchange con la routing key del evento. Así cada servicio recibe su copia del mensaje sin interferir con los demás.

## Formato de los mensajes

JSON en UTF-8, con este sobre. Los campos de `payload` son exactamente los que define la consigna: no se renombran ni se agregan.

```json
{
  "eventId": "7b1c1c1e-3f0a-4a8e-9d0b-2f6f6a3c9d11",
  "eventType": "reserva.confirmada",
  "correlationId": "b4a0e0c2-5a49-4b9e-8e53-0c1f4f1b6a77",
  "occurredAt": "2026-10-20T10:00:00",
  "payload": { }
}
```

| Campo | Qué es |
|---|---|
| `eventId` | UUID único de cada mensaje. Sirve para detectar duplicados. |
| `eventType` | Nombre del evento, igual a la routing key. |
| `correlationId` | UUID que une todos los eventos de un mismo proceso. Quien responde copia el del evento que recibió. |
| `occurredAt` | Fecha y hora de generación, ISO-8601 sin zona horaria, como en gRPC. |
| `payload` | Datos del evento, en camelCase, según las tablas de abajo. |

Reglas comunes:

- **Entrega al menos una vez.** RabbitMQ puede entregar un mensaje más de una vez, así que cada consumidor ignora un `eventId` que ya procesó.
- **Publicar después de guardar.** Se publica el evento una vez confirmada la transacción en la base, nunca antes.
- **Publicar no bloquea.** Si falla la publicación, la operación principal no se cancela: se registra el error y se sigue.
- **Mensajes persistentes** y confirmación (`ack`) después de procesarlos. Un mensaje mal formado se descarta sin reintentarlo.

## Flujo 1: verificación de patente al registrar un vehículo

| Evento | Publica | Consume | Cola | Payload |
|---|---|---|---|---|
| `vehiculos.verificacion.solicitada` | Vehicle Service | Registro policial simulado | `policia.vehiculos.verificacion.solicitada` | `vehiculoId`, `patente` |
| `vehiculos.verificacion.resultado` | Registro policial simulado | Vehicle Service | `vehicle.vehiculos.verificacion.resultado` | `vehiculoId`, `patente`, `resultado` |

`resultado`: `VALIDADO` o `ROBADO`. `ROBADO` no es un estado operativo del vehículo: si llega, el vehículo queda inhabilitado (`activo = false`) y el Gateway ya rechaza las reservas de vehículos inactivos.

```json
{ "vehiculoId": 15, "patente": "ABC123", "resultado": "VALIDADO" }
```

## Flujo 2: incorporación de vehículos de propietarios externos

El vehículo empieza en estado `PENDIENTE`. Si las dos validaciones dan `VALIDADO` se incorpora como `DISPONIBLE`; si alguna da `RECHAZADO`, no se incorpora. Los resultados de un mismo proceso se correlacionan por `vehiculoExternoId` (y por el `correlationId` del sobre).

| Evento | Publica | Consume | Cola | Payload |
|---|---|---|---|---|
| `vehiculos.externos.registrados` | Servicio que recibe la solicitud (a definir) | Registro policial y registro de titularidad | `policia.vehiculos.externos.registrados`, `titularidad.vehiculos.externos.registrados` | `vehiculoExternoId`, `clienteId`, `patente` |
| `vehiculos.externos.validacion.patente` | Registro policial simulado | Coordinador de la incorporación | `incorporacion.vehiculos.externos.validacion.patente` | `vehiculoExternoId`, `patente`, `resultado` |
| `vehiculos.externos.validacion.titularidad` | Registro de titularidad simulado | Coordinador de la incorporación | `incorporacion.vehiculos.externos.validacion.titularidad` | `vehiculoExternoId`, `clienteId`, `documento`, `patente`, `resultado` |
| `vehiculos.externos.validacion.completada` | Coordinador de la incorporación | Vehicle Service | `vehicle.vehiculos.externos.validacion.completada` | `vehiculoExternoId` |

`resultado` de las validaciones: `VALIDADO` o `RECHAZADO`. El evento `validacion.completada` se publica solo cuando llegaron las dos validaciones y ambas dieron `VALIDADO`.

Pendiente de acordar entre quienes lo desarrollan: qué servicio es el coordinador de la incorporación y quién guarda el proceso `PENDIENTE`.

```json
{ "vehiculoExternoId": 50, "clienteId": 89, "documento": "30123456", "patente": "DEF456", "resultado": "VALIDADO" }
```

## Flujo 3: mantenimiento preventivo de una reserva

La confirmación de la reserva no espera al mantenimiento. Rental confirma, publica el evento y responde; el resultado llega después.

| Evento | Publica | Consume | Cola | Payload |
|---|---|---|---|---|
| `reserva.confirmada` | Rental Service | Maintenance Service | `maintenance.reserva.confirmada` | `reservaId`, `vehiculoId`, `fechaInicio`, `fechaFin` |
| `mantenimiento.resultado` | Maintenance Service | Rental Service | `rental.mantenimiento.resultado` | `reservaId`, `vehiculoId`, `resultado` |

`resultado`: `REALIZADO` o `NO_REALIZADO`.

```json
{ "reservaId": 150, "vehiculoId": 15, "fechaInicio": "2026-10-10T10:00:00", "fechaFin": "2026-10-15T10:00:00" }
```

```json
{ "reservaId": 150, "vehiculoId": 15, "resultado": "REALIZADO" }
```

Cómo se ve en el resto del sistema:

- **Rental Service** guarda el resultado asociado a la reserva y lo informa en el campo `mantenimiento` de `Reservation` (`rental.proto`): `PENDIENTE` mientras no llegó el resultado, y después `REALIZADO` o `NO_REALIZADO`.
- **Maintenance Service** es un servicio nuevo en Java con Spring Boot, con su propia base `rentar_mantenimiento` donde registra cada solicitud, y sin API gRPC: solo consume y publica eventos.
- **Gateway y frontend** pueden mostrar el campo `mantenimiento` al consultar una reserva.

## Ejecucion local con Docker

Docker Desktop debe estar abierto solamente al probar la mensajeria real. Para crear
el broker local, se copia `.env.example` como `.env` si se desean definir credenciales
propias; si no existe `.env`, Docker Compose usa `guest` / `guest`.

```powershell
docker compose up -d
docker compose ps
```

Cuando el servicio figure como `healthy`, RabbitMQ queda disponible en
`localhost:5672` y su consola en `http://localhost:15672`. Para detenerlo sin
borrar los mensajes persistentes ni los datos del broker:

```powershell
docker compose stop
```

## Evidencia minima de pruebas

Para probar el flujo de mantenimiento se inicia RabbitMQ y luego Rental Service. Se confirma
una reserva con Maintenance Service detenido: la respuesta de la reserva debe llegar enseguida
y la cola `maintenance.reserva.confirmada` debe conservar el mensaje. Despues se inicia
Maintenance Service y se verifica que la solicitud se registra en `rentar_mantenimiento`, que
la cola se vacia y que Rental actualiza el campo `mantenimiento` a `REALIZADO`.

Para cubrir el segundo resultado se configura, antes de iniciar Maintenance Service:

```powershell
$env:MAINTENANCE_RESULTADO_NO_REALIZADO_VEHICLE_IDS = "<id-del-vehiculo>"
```

La evidencia a guardar es: consola de RabbitMQ con el exchange y las dos colas, respuesta de
la reserva confirmada sin esperar Maintenance, registros de ambos servicios y consulta final de
la reserva con `mantenimiento` en `REALIZADO` y en `NO_REALIZADO`. Tambien se conserva la salida
verde de los tests unitarios de Rental y Maintenance.

Para la verificacion de patente, se crea un vehiculo y se comprueba en RabbitMQ el paso por las
colas de solicitud y resultado. Luego se consulta `rentar_vehiculos.vehiculo` y
`evento_verificacion_procesado`: un resultado `VALIDADO` conserva `activo = true`; un resultado
`ROBADO` cambia el vehiculo a `activo = false` y deja el `event_id` registrado. Tambien se guarda
la salida verde de los tests unitarios de Vehicle Service.
