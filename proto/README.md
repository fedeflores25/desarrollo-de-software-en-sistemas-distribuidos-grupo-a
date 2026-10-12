# Contratos gRPC (Hito 2)

Un `.proto` por servicio. Son el contrato entre el API Gateway y los servicios internos.

| Archivo | Servicio | Responsabilidad |
|---|---|---|
| `vehicle.proto` | VehicleService | Vehiculos, disponibilidad y estado |
| `customer.proto` | CustomerService | Clientes, existencia y estado activo |
| `rental.proto` | RentalService | Reservas, cancelacion, historial y solapamiento |

## Convenciones

- Nombres de servicios, metodos y mensajes en ingles. Los campos usan los mismos nombres
  que la base de datos y el Hito 1 (`patente`, `precio_diario`, `fecha_inicio`...).
- Fechas como `string` ISO-8601 sin zona horaria: `"2026-10-05T10:00:00"` (fecha y hora)
  o `"1990-04-12"` (solo fecha).
- Importes como `double`.
- Los campos `optional` o `repeated` vacios no filtran.
- Los enums conservan los valores del Hito 1 (`SEDAN`, `DISPONIBLE`, `CONFIRMADA`...).
  El valor `*_UNSPECIFIED` (0) significa "sin informar".

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
