const assert = require('node:assert/strict');
const test = require('node:test');
const { buildSchema } = require('graphql');

const customer = require('../src/controllers/graphql/customerGraphQL');
const vehicle = require('../src/controllers/graphql/vehicleGraphQL');
const rental = require('../src/controllers/graphql/rentalGraphQL');

test('el esquema GraphQL del Gateway se construye con los contratos compartidos', () => {
    const schema = buildSchema(`
        ${customer.schemaString}
        ${vehicle.schemaString}
        ${rental.schemaString}

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

    assert.ok(schema.getType('DisponibilidadFiltroInput'));
    assert.ok(schema.getType('ReservaFiltroInput'));
    assert.ok(schema.getQueryType()?.getFields().vehiculosDisponibles);
});
