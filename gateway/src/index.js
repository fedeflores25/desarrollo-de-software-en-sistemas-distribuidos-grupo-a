const express = require('express');
const { graphqlHTTP } = require('express-graphql');
const { buildSchema } = require('graphql');

// 1. Importar los strings y funciones de GraphQL
const customerGraphQL = require('./controllers/graphql/customerGraphQL');
const vehicleGraphQL = require('./controllers/graphql/vehicleGraphQL');
const rentalGraphQL = require('./controllers/graphql/rentalGraphQL');

// 2. Importar los routers de REST
const customerRouter = require('./controllers/rest/customerController');
const vehicleRouter = require('./controllers/rest/vehicleController');
const rentalRouter = require('./controllers/rest/rentalController'); 

const app = express();
app.use(express.json());

// --- CONFIGURACIÓN GRAPHQL UNIFICADA ---
// Pegamos los "tipos" de cada archivo y declaramos un único "type Query" y "type Mutation"
const schema = buildSchema(`
  ${customerGraphQL.schemaString}
  ${vehicleGraphQL.schemaString}
  ${rentalGraphQL.schemaString}

  type Query {
    clientes(soloActivos: Boolean): [Cliente]
    cliente(id: ID!): Cliente
    
    vehiculos(filtro: VehiculoFiltroInput, soloActivos: Boolean): [Vehiculo]
    vehiculo(id: ID!): Vehiculo
    vehiculosDisponibles(filtro: DisponibilidadFiltroInput!): [Vehiculo]
    
    reservas(filtro: ReservaFiltroInput): [Reservation]
    reserva(id: ID!): Reservation
    historial(cliente_id: ID!): [Reservation]
  }

  type Mutation {
    crearCliente(input: ClienteInput!): Cliente
    desactivarCliente(id: ID!): Cliente
    crearReserva(input: CreateReservationInput!): Reservation
    cancelarReserva(id: ID!): Reservation
  }
`);

// Unimos todas las funciones (resolvers) en un solo objeto
const rootValue = {
    ...customerGraphQL.rootValue,
    ...vehicleGraphQL.rootValue,
    ...rentalGraphQL.rootValue
};

// Exponemos el único endpoint oficial de GraphQL
app.use('/graphql', graphqlHTTP({
  schema: schema,
  rootValue: rootValue,
  graphiql: true, // Interfaz visual en http://localhost:8080/graphql
}));

// --- CONFIGURACIÓN REST ---
app.use('/api/clientes', customerRouter);
app.use('/api/vehiculos', vehicleRouter);
app.use('/api/reservas', rentalRouter);

const PORT = process.env.PORT || 8080;
app.listen(PORT, () => {
    console.log(`🚀 API Gateway orquestando en el puerto ${PORT}`);
});
