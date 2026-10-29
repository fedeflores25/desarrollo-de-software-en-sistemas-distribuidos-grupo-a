const customerService = require('../../services/customerService');

const schemaString = `
  """
  Representa un cliente registrado en el sistema Rentar.
  """
  type Cliente {
    """Identificador único del cliente."""
    id: ID!

    """Número de documento del cliente."""
    documento: String

    """Nombre del cliente."""
    nombre: String

    """Apellido del cliente."""
    apellido: String

    """Correo electrónico del cliente."""
    email: String

    """Número de teléfono de contacto."""
    telefono: String

    """Fecha de nacimiento en formato ISO-8601 (YYYY-MM-DD)."""
    fecha_nacimiento: String

    """Indica si el cliente está activo en el sistema."""
    activo: Boolean
  }

  """
  Datos necesarios para registrar un nuevo cliente.
  Los campos obligatorios se indican con !.
  """
  input ClienteInput {
    """Documento único del cliente."""
    documento: String!

    """Nombre del cliente."""
    nombre: String!

    """Apellido del cliente."""
    apellido: String!

    """Correo electrónico único del cliente."""
    email: String!

    """Teléfono de contacto."""
    telefono: String!

    """Fecha de nacimiento en formato YYYY-MM-DD."""
    fecha_nacimiento: String
  }
`;

const rootValue = {
  clientes: async ({ soloActivos }) =>
    await customerService.listCustomers([], soloActivos),

  cliente: async ({ id }) =>
    await customerService.getCustomer(id),

  crearCliente: async ({ input }) =>
    await customerService.createCustomer(input),

  desactivarCliente: async ({ id }) =>
    await customerService.deactivateCustomer(id)
};

module.exports = { schemaString, rootValue };
