const rentalService = require('../../services/rentalService');

const schemaString = `
  """Estados posibles de una reserva"""
  enum EstadoReserva {
    CONFIRMADA
    CANCELADA
    FINALIZADA
  }

  type Reservation {
    id: ID!
    cliente_id: ID
    vehiculo_id: ID
    """Fecha en formato ISO-8601 (ej. 2026-10-05T10:00:00)"""
    fecha_inicio: String
    """Fecha en formato ISO-8601 (ej. 2026-10-05T10:00:00)"""
    fecha_fin: String
    precio_diario: Float
    cantidad_dias: Int
    importe_total: Float
    estado: EstadoReserva
    fecha_alta: String
    fecha_cancelacion: String
  }

  """Filtros para consultar reservas."""
  input ReservaFiltroInput {
    cliente_id: ID
    vehiculo_ids: [ID]
    estado: EstadoReserva
    """Filtrar desde esta fecha (ISO-8601)"""
    desde: String 
    """Filtrar hasta esta fecha (ISO-8601)"""
    hasta: String 
  }

  input CreateReservationInput {
    cliente_id: ID!
    vehiculo_id: ID!
    """Fecha en formato ISO-8601 (ej. 2026-10-05T10:00:00)"""
    fecha_inicio: String!
    """Fecha en formato ISO-8601 (ej. 2026-10-05T10:00:00)"""
    fecha_fin: String!
  }
`;

const rootValue = {
    reservas: async ({ filtro }) => {
        const response = await rentalService.listReservations(filtro || {});
        return response.reservas || [];
    },
    reserva: async ({ id }) => {
        return await rentalService.getReservation(id);
    },
    historial: async ({ cliente_id }) => {
        const response = await rentalService.getRentalHistory(cliente_id);
        return response.reservas || [];
    },
    crearReserva: async ({ input }) => {
        return await rentalService.createReservation(input);
    },
    cancelarReserva: async ({ id }) => {
        return await rentalService.cancelReservation(id);
    }
};

module.exports = { schemaString, rootValue };