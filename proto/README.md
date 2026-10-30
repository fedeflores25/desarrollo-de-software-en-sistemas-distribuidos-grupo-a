# Contratos gRPC (Hito 2)

Un `.proto` por servicio. Son el contrato entre el API Gateway y los servicios internos.

| Archivo | Servicio | Responsabilidad |
|---|---|---|
| `vehicle.proto` | VehicleService | Vehiculos, disponibilidad y estado |
| `customer.proto` | CustomerService | Clientes, existencia y estado activo |
| `rental.proto` | RentalService | Reservas, cancelacion, historial y solapamiento |

## Convenciones

- Nombres de servicios, metodos y mensajes en ingles. Los campos usan los mismos nombres que la base de datos y el Hito 1 (`patente`, `precio_diario`, `fecha_inicio`...).
- Las fechas se intercambian como `string` en formato ISO-8601, sin zona horaria ni desplazamiento UTC. Los formatos se detallan en la siguiente seccion.
- Importes como `double`.
- Los campos `optional` o `repeated` vacios no filtran.
- Los enums conservan los valores del Hito 1 (`SEDAN`, `DISPONIBLE`, `CONFIRMADA`...), utilizando los prefijos definidos en cada contrato `.proto`. El valor `*_UNSPECIFIED` (0) significa "sin informar".

## Convenciones de fecha y hora

Las fechas se representan mediante cadenas de texto (`string`) siguiendo ISO-8601.

Se distinguen dos formatos de intercambio:

- Solo fecha: `YYYY-MM-DD`.
- Fecha y hora: `YYYY-MM-DDTHH:mm:ss`.

Los valores no incluyen zona horaria ni desplazamiento UTC.

### Formatos por campo

| Campo | Formato | Ejemplo |
|---|---|---|
| `fecha_nacimiento` | `YYYY-MM-DD` | `2000-03-20` |
| `fecha_inicio` | `YYYY-MM-DDTHH:mm:ss` | `2026-11-05T10:00:00` |
| `fecha_fin` | `YYYY-MM-DDTHH:mm:ss` | `2026-11-08T10:00:00` |
| `fecha_alta` | `YYYY-MM-DDTHH:mm:ss` | `2026-10-30T09:15:00` |
| `fecha_cancelacion` | `YYYY-MM-DDTHH:mm:ss` | `2026-11-01T14:30:00` |
| `desde` | `YYYY-MM-DDTHH:mm:ss` | `2026-11-01T00:00:00` |
| `hasta` | `YYYY-MM-DDTHH:mm:ss` | `2026-11-30T23:59:59` |

### Reglas de representacion

- `fecha_nacimiento` representa una fecha sin componente horario.
- Si `fecha_nacimiento` no se informa, se representa mediante una cadena vacia.
- Las fechas del Rental Service representan fecha y hora local, sin informacion de zona horaria.
- Las respuestas del Rental Service incluyen segundos en sus campos de fecha y hora.
- `fecha_alta` corresponde al momento de registro de la reserva.
- `fecha_cancelacion` devuelve una cadena vacia cuando la reserva no fue cancelada.
- Los filtros `desde` y `hasta` permiten consultar reservas dentro de un rango temporal.
- El Gateway debe conservar las convenciones de fecha al intercambiar datos con los servicios.

### Validaciones

Al crear una reserva se aplican las siguientes reglas:

1. `fecha_inicio` y `fecha_fin` deben contener fechas validas.
2. `fecha_fin` debe ser posterior a `fecha_inicio`.
3. `fecha_inicio` debe corresponder a un momento futuro.
4. El periodo solicitado no debe superponerse con reservas confirmadas del mismo vehiculo.

Las fechas invalidas generan el codigo gRPC `INVALID_ARGUMENT`.

Los conflictos por ocupacion del vehiculo generan `FAILED_PRECONDITION`.

### Ejemplo de solicitud

```json
{
  "cliente_id": 1,
  "vehiculo_id": 3,
  "fecha_inicio": "2026-11-05T10:00:00",
  "fecha_fin": "2026-11-08T10:00:00",
  "precio_diario": 45000
}
```

### Consideraciones

- El formato documentado para el intercambio de fechas con hora incluye segundos.
- No debe agregarse automaticamente el sufijo `Z`, ya que indicaria UTC.
- Las fechas sin zona horaria no deben interpretarse automaticamente como UTC.
- REST, GraphQL y gRPC deben mantener una representacion coherente al intercambiar estos valores.

## Errores

Los servicios responden con codigos de estado gRPC y el Gateway los traduce a HTTP:

| Codigo gRPC | Cuando | HTTP |
|---|---|---|
| `INVALID_ARGUMENT` | Datos faltantes o invalidos (fechas incoherentes) | 400 |
| `NOT_FOUND` | El recurso no existe | 404 |
| `ALREADY_EXISTS` | Patente, documento o email repetidos | 409 |
| `FAILED_PRECONDITION` | Estado no permite la operacion (inactivo, ocupado, ya comenzo) | 409 |

## Flujo de una reserva

1. Gateway -> `CustomerService.VerifyCustomer` (existe y esta activo)
2. Gateway -> `VehicleService.GetVehicle` (existe, esta activo, precio diario)
3. Gateway -> `RentalService.CreateReservation` (valida fechas y solapamiento, calcula importe)

## Disponibilidad

1. Gateway -> `RentalService.GetBusyVehicleIds` (vehiculos ocupados en el periodo)
2. Gateway -> `VehicleService.ListAvailableVehicles` (activos, con filtros, excluyendo los ocupados)

## Eventos (Hito 3)

Los eventos asincronicos (RabbitMQ) no se definen en archivos `.proto`: su formato, los nombres, las colas y los payloads estan en [`docs/eventos.md`](../docs/eventos.md).

`Reservation` en `rental.proto` suma el campo `mantenimiento` (`PENDIENTE`, `REALIZADO` o `NO_REALIZADO`), que informa el resultado del mantenimiento preventivo de la reserva.
