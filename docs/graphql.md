# Documentación de la API GraphQL

## 1. Descripción general

El API Gateway del sistema Rentar expone una API GraphQL que permite consultar clientes, vehículos y reservas, además de realizar operaciones de creación y cancelación.

El Gateway recibe las solicitudes GraphQL y se comunica con los microservicios mediante gRPC.

- Endpoint: `http://localhost:8080/graphql`
- Método HTTP: POST
- Formato de intercambio: JSON
- Herramienta de exploración: GraphiQL

La documentación interactiva del esquema se encuentra disponible en el panel `Docs` de GraphiQL.

## 2. Consultas disponibles

| Consulta | Descripción |
|---|---|
| `clientes(soloActivos)` | Lista clientes y permite filtrar los activos. |
| `cliente(id)` | Consulta un cliente por su identificador. |
| `vehiculos(filtro, soloActivos)` | Lista vehículos aplicando filtros opcionales. |
| `vehiculo(id)` | Consulta un vehículo por su identificador. |
| `vehiculosDisponibles(filtro)` | Consulta vehículos disponibles para un período. |
| `reservas(filtro)` | Consulta reservas aplicando filtros. |
| `reserva(id)` | Obtiene una reserva por su identificador. |
| `historial(cliente_id)` | Obtiene el historial de reservas de un cliente. |

### 2.1. Consultar clientes activos

Consulta:

```graphql
query {
  clientes(soloActivos: true) {
    id
    documento
    nombre
    apellido
    email
    activo
  }
}
```

Ejemplo de respuesta:

```json
{
  "data": {
    "clientes": [
      {
        "id": "1",
        "documento": "30111222",
        "nombre": "Ana",
        "apellido": "García",
        "email": "ana@example.com",
        "activo": true
      }
    ]
  }
}
```

### 2.2. Consultar vehículos con filtros

El tipo `VehiculoFiltroInput` permite filtrar por tipo, marca, modelo, precio mínimo y precio máximo.

Consulta:

```graphql
query {
  vehiculos(
    filtro: {
      tipo: SEDAN
      marca: "Toyota"
      precio_max: 50000
    }
    soloActivos: true
  ) {
    id
    patente
    marca
    modelo
    tipo
    precio_diario
    estado
  }
}
```

Ejemplo de respuesta:

```json
{
  "data": {
    "vehiculos": [
      {
        "id": "15",
        "patente": "ABC123",
        "marca": "Toyota",
        "modelo": "Corolla",
        "tipo": "SEDAN",
        "precio_diario": 45000,
        "estado": "DISPONIBLE"
      }
    ]
  }
}
```

### 2.3. Consultar disponibilidad de vehículos

El filtro `DisponibilidadFiltroInput` requiere fecha de inicio y fecha de fin. También permite filtrar por características y precios del vehículo.

Las fechas utilizan ISO-8601 sin zona horaria, con formato `YYYY-MM-DDTHH:mm:ss`.

Consulta:

```graphql
query {
  vehiculosDisponibles(
    filtro: {
      fecha_inicio: "2026-11-10T10:00:00"
      fecha_fin: "2026-11-12T10:00:00"
      tipo: SEDAN
    }
  ) {
    id
    patente
    marca
    modelo
    estado
    precio_diario
  }
}
```

Ejemplo de respuesta:

```json
{
  "data": {
    "vehiculosDisponibles": [
      {
        "id": "15",
        "patente": "ABC123",
        "marca": "Toyota",
        "modelo": "Corolla",
        "estado": "DISPONIBLE",
        "precio_diario": 45000
      }
    ]
  }
}
```

La consulta utiliza Rental Service para identificar los vehículos ocupados durante el período y Vehicle Service para obtener los disponibles.

### 2.4. Consultar reservas con filtros

El tipo `ReservaFiltroInput` permite filtrar por cliente, vehículo, tipo de vehículo, estado y rango de fechas.

Consulta:

```graphql
query {
  reservas(
    filtro: {
      cliente_id: "1"
      estado: CONFIRMADA
    }
  ) {
    id
    cliente
    vehiculo
    patente
    fecha_inicio
    fecha_fin
    importe_total
    estado
  }
}
```

Ejemplo de respuesta:

```json
{
  "data": {
    "reservas": [
      {
        "id": "150",
        "cliente": "Ana García",
        "vehiculo": "Toyota Corolla",
        "patente": "ABC123",
        "fecha_inicio": "2026-11-10T10:00:00",
        "fecha_fin": "2026-11-12T10:00:00",
        "importe_total": 90000,
        "estado": "CONFIRMADA"
      }
    ]
  }
}
```

### 2.5. Consultar el historial de un cliente

Consulta:

```graphql
query {
  historial(cliente_id: "1") {
    id
    vehiculo
    fecha_inicio
    fecha_fin
    estado
    importe_total
  }
}
```

Ejemplo de respuesta:

```json
{
  "data": {
    "historial": [
      {
        "id": "150",
        "vehiculo": "Toyota Corolla",
        "fecha_inicio": "2026-11-10T10:00:00",
        "fecha_fin": "2026-11-12T10:00:00",
        "estado": "CONFIRMADA",
        "importe_total": 90000
      }
    ]
  }
}
```

## 3. Mutaciones disponibles

| Mutación | Descripción |
|---|---|
| `crearCliente(input)` | Registra un cliente. |
| `desactivarCliente(id)` | Realiza la baja lógica de un cliente. |
| `crearReserva(input)` | Crea una reserva. |
| `cancelarReserva(id)` | Cancela una reserva existente. |

### 3.1. Crear un cliente

Consulta:

```graphql
mutation {
  crearCliente(input: {
    documento: "30111222"
    nombre: "Ana"
    apellido: "García"
    email: "ana@example.com"
    telefono: "3815551234"
    fecha_nacimiento: "1990-04-12"
  }) {
    id
    nombre
    apellido
    email
    activo
  }
}
```

Ejemplo de respuesta:

```json
{
  "data": {
    "crearCliente": {
      "id": "1",
      "nombre": "Ana",
      "apellido": "García",
      "email": "ana@example.com",
      "activo": true
    }
  }
}
```

### 3.2. Desactivar un cliente

Consulta:

```graphql
mutation {
  desactivarCliente(id: "1") {
    id
    nombre
    activo
  }
}
```

Ejemplo de respuesta:

```json
{
  "data": {
    "desactivarCliente": {
      "id": "1",
      "nombre": "Ana",
      "activo": false
    }
  }
}
```

La operación realiza una baja lógica sin eliminar físicamente el cliente.

### 3.3. Crear una reserva

Consulta:

```graphql
mutation {
  crearReserva(input: {
    cliente_id: "1"
    vehiculo_id: "15"
    fecha_inicio: "2026-11-10T10:00:00"
    fecha_fin: "2026-11-12T10:00:00"
  }) {
    id
    cliente
    vehiculo
    fecha_inicio
    fecha_fin
    importe_total
    estado
  }
}
```

Ejemplo de respuesta:

```json
{
  "data": {
    "crearReserva": {
      "id": "150",
      "cliente": "Ana García",
      "vehiculo": "Toyota Corolla",
      "fecha_inicio": "2026-11-10T10:00:00",
      "fecha_fin": "2026-11-12T10:00:00",
      "importe_total": 90000,
      "estado": "CONFIRMADA"
    }
  }
}
```

Para confirmar la reserva se verifica que el cliente exista y esté activo, que el vehículo esté disponible y que el período sea válido.

### 3.4. Cancelar una reserva

Consulta:

```graphql
mutation {
  cancelarReserva(id: "150") {
    id
    estado
    fecha_cancelacion
  }
}
```

Ejemplo de respuesta:

```json
{
  "data": {
    "cancelarReserva": {
      "id": "150",
      "estado": "CANCELADA",
      "fecha_cancelacion": "2026-11-05T12:00:00"
    }
  }
}
```

## 4. Tipos de datos y filtros

Los tipos están definidos y documentados en los siguientes archivos:

- `gateway/src/controllers/graphql/customerGraphQL.js`
- `gateway/src/controllers/graphql/vehicleGraphQL.js`
- `gateway/src/controllers/graphql/rentalGraphQL.js`
- `gateway/src/index.js`

Se utilizan los siguientes tipos:

| Tipo | Finalidad |
|---|---|
| `Cliente` | Representación de un cliente. |
| `ClienteInput` | Datos para registrar un cliente. |
| `Vehiculo` | Representación de un vehículo. |
| `TipoVehiculo` | Enumerado de tipos de vehículos. |
| `VehiculoFiltroInput` | Filtros de búsqueda de vehículos. |
| `DisponibilidadFiltroInput` | Período y filtros de disponibilidad. |
| `Reservation` | Representación de una reserva. |
| `EstadoReserva` | Enumerado de estados de reserva. |
| `ReservaFiltroInput` | Filtros de búsqueda de reservas. |
| `CreateReservationInput` | Datos para crear una reserva. |

El esquema completo, las descripciones y los argumentos de cada operación se pueden consultar desde `Docs` en GraphiQL.

## 5. Verificación

Para verificar las operaciones:

1. Iniciar el API Gateway y los microservicios correspondientes.
2. Acceder a `http://localhost:8080/graphql`.
3. Copiar una consulta o mutación en el editor izquierdo.
4. Ejecutarla mediante el botón de reproducción.
5. Revisar la respuesta JSON y los posibles errores.

Los ejemplos de respuesta de este documento son ilustrativos. Los identificadores, importes y resultados efectivos dependen de los datos registrados y del estado de los servicios.

Para ejecutar mutaciones se deben utilizar registros adecuados para cada escenario. Una baja o cancelación modifica el estado persistido.
