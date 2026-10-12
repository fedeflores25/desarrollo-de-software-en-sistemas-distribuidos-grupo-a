const customerService = require('../../services/customerService');

const schemaString = `
  type Cliente {
    id: ID!
    documento: String
    nombre: String
    apellido: String
    email: String
    telefono: String
    fecha_nacimiento: String
    activo: Boolean
  }

  input ClienteInput {
    documento: String!
    nombre: String!
    apellido: String!
    email: String!
    telefono: String!
    fecha_nacimiento: String
  }
`;

const rootValue = {
    clientes: async ({ soloActivos }) => await customerService.listCustomers([], soloActivos),
    cliente: async ({ id }) => await customerService.getCustomer(id),
    crearCliente: async ({ input }) => await customerService.createCustomer(input),
    desactivarCliente: async ({ id }) => await customerService.deactivateCustomer(id)
};

module.exports = { schemaString, rootValue };