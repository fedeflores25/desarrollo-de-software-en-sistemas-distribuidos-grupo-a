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
const authRouter = require('./controllers/rest/authController');
const { authenticate, getOptionalUser } = require('./middleware/auth');

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

function requireGraphqlUser(user) {
  if (!user) throw new Error('Se requiere una sesion autenticada.');
}

function requireGraphqlAdmin(user) {
  requireGraphqlUser(user);
  if (user.rol !== 'ADMIN') throw new Error('No tiene permisos para esta operacion.');
}

function buildRootValue(req) {
  const user = getOptionalUser(req);
  const admin = (resolver) => (args) => {
    requireGraphqlAdmin(user);
    return resolver(args);
  };
  const ownReservation = async (id) => {
    const reserva = await rentalGraphQL.rootValue.reserva({ id });
    if (user.rol === 'CLIENTE' && Number(reserva.cliente_id) !== Number(user.clienteId)) {
      throw new Error('No tiene permisos para operar sobre esta reserva.');
    }
    return reserva;
  };

  return {
    ...rootValue,
    clientes: admin(customerGraphQL.rootValue.clientes),
    cliente: admin(customerGraphQL.rootValue.cliente),
    crearCliente: admin(customerGraphQL.rootValue.crearCliente),
    desactivarCliente: admin(customerGraphQL.rootValue.desactivarCliente),
    vehiculos: admin(vehicleGraphQL.rootValue.vehiculos),
    vehiculo: admin(vehicleGraphQL.rootValue.vehiculo),
    vehiculosDisponibles: (args) => {
      requireGraphqlUser(user);
      return vehicleGraphQL.rootValue.vehiculosDisponibles(args);
    },
    reservas: (args) => {
      requireGraphqlUser(user);
      const filtro = user.rol === 'CLIENTE'
        ? { ...(args.filtro || {}), cliente_id: user.clienteId }
        : args.filtro;
      return rentalGraphQL.rootValue.reservas({ filtro });
    },
    reserva: async ({ id }) => {
      requireGraphqlUser(user);
      return ownReservation(id);
    },
    historial: ({ cliente_id }) => {
      requireGraphqlUser(user);
      const clienteId = user.rol === 'CLIENTE' ? user.clienteId : cliente_id;
      return rentalGraphQL.rootValue.historial({ cliente_id: clienteId });
    },
    crearReserva: ({ input }) => {
      requireGraphqlUser(user);
      const reserva = user.rol === 'CLIENTE'
        ? { ...input, cliente_id: user.clienteId }
        : input;
      return rentalGraphQL.rootValue.crearReserva({ input: reserva });
    },
    cancelarReserva: async ({ id }) => {
      requireGraphqlUser(user);
      await ownReservation(id);
      return rentalGraphQL.rootValue.cancelarReserva({ id });
    },
  };
}

// Configuracion del endpoint GraphQL.
app.use('/graphql', graphqlHTTP((req) => ({
  schema,
  rootValue: buildRootValue(req),
  graphiql: true,
})));

// Documentacion REST mediante Swagger/OpenAPI.
app.get('/api-docs.json', (req, res) => res.json(openapi));

app.use(
  '/api-docs',
  swaggerUi.serve,
  swaggerUi.setup(openapi)
);


// Registro de rutas REST.
app.use('/api/auth', authRouter);
app.use('/api/clientes', authenticate, customerRouter);
app.use('/api/vehiculos', authenticate, vehicleRouter);
app.use('/api/reservas', authenticate, rentalRouter);

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

