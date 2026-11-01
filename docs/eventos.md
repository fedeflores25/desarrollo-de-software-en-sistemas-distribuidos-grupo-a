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

JSON en UTF-8, con este sobre. Los campos de `payload` se mantienen según el contrato acordado por el equipo. Deben compararse con los ejemplos de la consigna original antes de entregar; este documento describe lo implementado en esta rama.

```json
{
  "eventId": "7b1c1c1e-3f0a-4a8e-9d0b-2f6f6a3c9d11",
  "eventType": "reserva.confirmada",
  "correlationId": "b4a0e0c2-5a49-4b9e-8e53-0c1f4f1b6a77",
  "occurredAt": "2026-10-20T10:00:00",
  "payload": {}
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

- **Entrega al menos una vez.** RabbitMQ puede entregar un mensaje repetido. `eventId` identifica cada mensaje, pero **no todos los consumidores persisten los `eventId` procesados**. En el flujo externo, Vehicle Service evita aplicar dos veces una validación ya guardada y evita incorporar dos veces una solicitud `INCORPORADA`; Titularidad puede volver a emitir un resultado si recibe de nuevo el registro. Esto brinda idempotencia funcional para las operaciones principales, pero **no es deduplicación general por `eventId`**.
- **Publicar después de guardar.** Los publicadores de eventos de Vehicle Service para este flujo actúan después de confirmar la transacción mediante `AFTER_COMMIT`; Titularidad publica el resultado al procesar el evento recibido.
- **No bloquear la solicitud inicial.** El registro de la solicitud externa no espera las validaciones. Si falla la publicación, se registra el error; las solicitudes que sigan `PENDIENTE` se revisan mediante la recuperación programada. Esto no equivale a una garantía de entrega infalible.
- **Mensajes persistentes** y confirmación (`ack`) después de procesarlos. Los consumidores del flujo externo están configurados con `acknowledge-mode=manual`: rechazan un mensaje inválido y reencolan errores de procesamiento que puedan recuperarse (salvo el tratamiento específico del consumidor de `validacion.completada`, que utiliza recuperación programada).

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

La **solicitud externa** empieza en estado `PENDIENTE`. Si las dos validaciones dan `VALIDADO`, se crea un vehículo con estado operativo `DISPONIBLE` y la solicitud pasa a `INCORPORADA`. Si alguna da `RECHAZADO`, la solicitud pasa a `RECHAZADA` y no se crea el vehículo.

`PENDIENTE`, `RECHAZADA` e `INCORPORADA` son estados de **la solicitud**, no del vehículo.

Los resultados se correlacionan por `vehiculoExternoId` y por el `correlationId` del sobre. En esta implementación, Vehicle Service calcula un UUID reproducible a partir de `vehiculo-externo:<vehiculoExternoId>`; los otros servicios copian ese valor al responder.

| Evento | Publica | Consume | Cola | Payload |
|---|---|---|---|---|
| `vehiculos.externos.registrados` | Vehicle Service | Registro policial y registro de titularidad | `policia.vehiculos.externos.registrados`, `titularidad.vehiculos.externos.registrados` | `vehiculoExternoId`, `clienteId`, `patente` |
| `vehiculos.externos.validacion.patente` | Registro policial simulado | Vehicle Service (coordinador) | `incorporacion.vehiculos.externos.validacion.patente` | `vehiculoExternoId`, `patente`, `resultado` |
| `vehiculos.externos.validacion.titularidad` | Registro de titularidad simulado | Vehicle Service (coordinador) | `incorporacion.vehiculos.externos.validacion.titularidad` | `vehiculoExternoId`, `clienteId`, `documento`, `patente`, `resultado` |
| `vehiculos.externos.validacion.completada` | Vehicle Service (coordinador) | Vehicle Service (incorporador) | `vehicle.vehiculos.externos.validacion.completada` | `vehiculoExternoId` |

`resultado` de las validaciones: `VALIDADO` o `RECHAZADO`. El evento `validacion.completada` se publica solo cuando llegaron las dos validaciones y ambas dieron `VALIDADO`.

**Responsabilidades ya implementadas:** Vehicle Service recibe y guarda la solicitud `PENDIENTE`, publica `vehiculos.externos.registrados`, consume y relaciona ambos resultados, publica `vehiculos.externos.validacion.completada` únicamente cuando las dos respuestas son `VALIDADO`, y luego consume ese evento para incorporar el vehículo.

Titularidad Service consume `vehiculos.externos.registrados` y publica su resultado con `documento` obtenido del registro simulado. La validación policial simulada y su respuesta de patente externa se coordinan con Jonathan.

La solicitud original contiene `documento`, pero el evento `vehiculos.externos.registrados` no lo envía: el registro de titularidad simulado retorna el documento registrado y Vehicle Service lo compara contra el documento de la solicitud. Por eso una solicitud con documento incorrecto termina `RECHAZADA`, aunque `clienteId` y `patente` correspondan a un titular conocido.

**Recuperación:** Vehicle Service revisa periódicamente las solicitudes `PENDIENTE`. Si faltan resultados, vuelve a publicar `vehiculos.externos.registrados`; si ambas validaciones ya constan como `VALIDADO`, vuelve a publicar `vehiculos.externos.validacion.completada`.

Estos reenvíos pueden producir mensajes repetidos, por lo que se controla el estado persistido antes de efectuar cambios.

```json
{ "vehiculoExternoId": 50, "clienteId": 89, "documento": "30123456", "patente": "DEF456", "resultado": "VALIDADO" }
```

## Prueba mínima de incorporación externa

1. Levantar RabbitMQ, Vehicle Service y Titularidad Service; para completar el flujo se necesita además el registro policial simulado configurado para consumir `vehiculos.externos.registrados` y publicar `vehiculos.externos.validacion.patente`.
2. Crear una solicitud con patente `EXT101`, `clienteId = 1` y `documento = "30111222"` (junto con los demás campos obligatorios de vehículo). La solicitud inicialmente debe ser `PENDIENTE`.
3. Verificar que la validación de titularidad produzca `VALIDADO`. Si la policía también responde `VALIDADO`, comprobar la publicación de `vehiculos.externos.validacion.completada`, la solicitud `INCORPORADA` con `vehiculoId` asignado y el nuevo vehículo con estado `DISPONIBLE`.
4. Repetir la demostración en una solicitud nueva con patente `EXT102` y un cliente diferente del titular simulado (`clienteId != 2`), o con documento incorrecto. Debe quedar `RECHAZADA`, sin crear vehículo.
5. Enviar las dos validaciones en orden inverso para confirmar que el orden de llegada no cambia el resultado. Comprobar además que una entrega repetida no incorpora dos veces el mismo vehículo.

Los casos de prueba presuponen una base sin esas patentes incorporadas previamente. El Gateway incluye el registro policial simulado: consume `vehiculos.externos.registrados` y publica `vehiculos.externos.validacion.patente` con el mismo `correlationId`.

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

Docker Desktop debe estar abierto solamente al probar la mensajeria real. Para crear el broker local, se copia `.env.example` como `.env` si se desean definir credenciales propias; si no existe `.env`, Docker Compose usa `guest` / `guest`.

```powershell
docker compose up -d
docker compose ps
```

Cuando el servicio figure como `healthy`, RabbitMQ queda disponible en `localhost:5672` y su consola en `http://localhost:15672`. Para detenerlo sin borrar los mensajes persistentes ni los datos del broker:

```powershell
docker compose stop
```

## Evidencia minima de pruebas

Para probar el flujo de mantenimiento se inicia RabbitMQ y luego Rental Service. Se confirma una reserva con Maintenance Service detenido: la respuesta de la reserva debe llegar enseguida y la cola `maintenance.reserva.confirmada` debe conservar el mensaje. Despues se inicia Maintenance Service y se verifica que la solicitud se registra en `rentar_mantenimiento`, que la cola se vacia y que Rental actualiza el campo `mantenimiento` a `REALIZADO`.

Para cubrir el segundo resultado se configura, antes de iniciar Maintenance Service:

```powershell
$env:MAINTENANCE_RESULTADO_NO_REALIZADO_VEHICLE_IDS = "<id-del-vehiculo>"
```

La evidencia a guardar es: consola de RabbitMQ con el exchange y las dos colas, respuesta de la reserva confirmada sin esperar Maintenance, registros de ambos servicios y consulta final de la reserva con `mantenimiento` en `REALIZADO` y en `NO_REALIZADO`. Tambien se conserva la salida verde de los tests unitarios de Rental y Maintenance.

Para la verificacion de patente, se crea un vehiculo y se comprueba en RabbitMQ el paso por las colas de solicitud y resultado. Luego se consulta `rentar_vehiculos.vehiculo` y `evento_verificacion_procesado`: un resultado `VALIDADO` conserva `activo = true`; un resultado `ROBADO` cambia el vehiculo a `activo = false` y deja el `event_id` registrado. Tambien se guarda la salida verde de los tests unitarios de Vehicle Service.
