const express = require('express');
const { graphqlHTTP } = require('express-graphql');
const { buildSchema } = require('graphql');

// Documentacion Swagger/OpenAPI.
const swaggerUi = require('swagger-ui-express');
const openapi = require('./openapi');

// Esquemas y resolvers GraphQL.
const customerGraphQL = require('./controllers/graphql/customerGraphQL');
const vehicleGraphQL = require('./controllers/graphql/vehicleGraphQL');
const rentalGraphQL = require('./controllers/graphql/rentalGraphQL');

// Controladores REST.
const customerRouter = require('./controllers/rest/customerController');
const vehicleRouter = require('./controllers/rest/vehicleController');
const rentalRouter = require('./controllers/rest/rentalController');

const app = express();
app.use(express.json());

// Configuracion del esquema GraphQL unificado.
const schema = buildSchema(`
  ${customerGraphQL.schemaString}
  ${vehicleGraphQL.schemaString}
  ${rentalGraphQL.schemaString}

  """Consultas disponibles para clientes, vehículos y reservas."""
  type Query {

    """Lista clientes y permite consultar solamente los activos."""
    clientes(
      """Si es true, devuelve solamente clientes activos."""
      soloActivos: Boolean
    ): [Cliente]

    """Consulta un cliente por su identificador."""
    cliente(
      """Identificador del cliente."""
      id: ID!
    ): Cliente

    """Lista vehículos aplicando filtros opcionales."""
    vehiculos(
      """Filtros por tipo, marca, modelo y precio."""
      filtro: VehiculoFiltroInput

      """Si es true, devuelve solamente vehículos activos."""
      soloActivos: Boolean
    ): [Vehiculo]

    """Consulta un vehículo por su identificador."""
    vehiculo(
      """Identificador del vehículo."""
      id: ID!
    ): Vehiculo

    """
    Consulta los vehículos disponibles en un período.
    Considera las fechas solicitadas y los filtros opcionales.
    """
    vehiculosDisponibles(
      """Período y filtros de búsqueda."""
      filtro: DisponibilidadFiltroInput!
    ): [Vehiculo]

    """Lista reservas aplicando filtros opcionales."""
    reservas(
      """Filtros por cliente, vehículo, tipo, estado y fechas."""
      filtro: ReservaFiltroInput
    ): [Reservation]

    """Consulta una reserva por su identificador."""
    reserva(
      """Identificador de la reserva."""
      id: ID!
    ): Reservation

    """Obtiene el historial de reservas de un cliente."""
    historial(
      """Identificador del cliente."""
      cliente_id: ID!
    ): [Reservation]
  }

  """Operaciones para crear y modificar recursos."""
  type Mutation {

    """
    Registra un nuevo cliente.
    Valida los datos obligatorios y la unicidad
    del documento y del correo electrónico.
    """
    crearCliente(
      """Datos del cliente que se desea registrar."""
      input: ClienteInput!
    ): Cliente

    """
    Realiza la baja lógica de un cliente.
    El cliente permanece registrado como inactivo.
    """
    desactivarCliente(
      """Identificador del cliente."""
      id: ID!
    ): Cliente

    """
    Crea una reserva para un cliente y un vehículo.
    Verifica las condiciones necesarias para confirmar
    la operación.
    """
    crearReserva(
      """Cliente, vehículo y período solicitado."""
      input: CreateReservationInput!
    ): Reservation

    """
    Cancela una reserva existente.
    La operación respeta las restricciones
    del estado actual de la reserva.
    """
    cancelarReserva(
      """Identificador de la reserva."""
      id: ID!
    ): Reservation
  }
`);

// Unificacion de resolvers.
const rootValue = {
  ...customerGraphQL.rootValue,
  ...vehicleGraphQL.rootValue,
  ...rentalGraphQL.rootValue
};

// Configuracion del endpoint GraphQL.
app.use('/graphql', graphqlHTTP({
  schema: schema,
  rootValue: rootValue,
  graphiql: true,
}));

// Documentacion REST mediante Swagger/OpenAPI.
app.get('/api-docs.json', (req, res) => res.json(openapi));

app.use(
  '/api-docs',
  swaggerUi.serve,
  swaggerUi.setup(openapi)
);


// Registro de rutas REST.
app.use('/api/clientes', customerRouter);
app.use('/api/vehiculos', vehicleRouter);
app.use('/api/reservas', rentalRouter);

const { connectRabbitMQ } = require('./config/rabbitmq'); // <--  Importar la conexión
const { iniciarRegistroPolicial } = require('./events/verificacionPolicial'); // <-- Importar la función para iniciar el registro policial

const PORT = process.env.PORT || 8080;

// conectar a RabbitMQ y  levantar el servidor web 
connectRabbitMQ().then(() => {
    iniciarRegistroPolicial(); // <-- Llamar a la función para iniciar el registro policial (consumidor)
    app.listen(PORT, () => {
        console.log(`API Gateway orquestando en el puerto ${PORT}`);
        console.log(`Swagger disponible en http://localhost:${PORT}/api-docs`);
        console.log(`GraphQL disponible en http://localhost:${PORT}/graphql`);
    });
}).catch(error => {
    console.error('Error crítico al iniciar RabbitMQ:', error);
});

