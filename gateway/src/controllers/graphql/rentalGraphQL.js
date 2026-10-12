const rentalService = require('../../services/rentalService');
const customerService = require('../../services/customerService');
const vehicleService = require('../../services/vehicleService');

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
    cliente: String
    vehiculo: String
    patente: String
  }

  """Filtros para consultar reservas."""
  input ReservaFiltroInput {
    cliente_id: ID
    vehiculo_id: ID
    tipo_vehiculo: TipoVehiculo
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

async function toRentalFilter(filtro) {
    const { vehiculo_id, tipo_vehiculo, ...rentalFilter } = filtro || {};
    let vehicleIds;

    if (vehiculo_id) {
        vehicleIds = new Set([String(vehiculo_id)]);
    }

    if (tipo_vehiculo) {
        const vehicles = await vehicleService.listVehicles({ tipo: tipo_vehiculo }, [], true);
        const typeVehicleIds = new Set(vehicles.map((vehicle) => String(vehicle.id)));
        vehicleIds = vehicleIds
            ? new Set([...vehicleIds].filter((id) => typeVehicleIds.has(id)))
            : typeVehicleIds;
    }

    if (vehicleIds) {
        rentalFilter.vehiculo_ids = [...vehicleIds];
    }

    return rentalFilter;
}

async function enrichReservation(reserva) {
    const [cliente, vehiculo] = await Promise.all([
        customerService.getCustomer(reserva.cliente_id),
        vehicleService.getVehicle(reserva.vehiculo_id),
    ]);

    return {
        ...reserva,
        cliente: `${cliente.nombre} ${cliente.apellido}`.trim(),
        vehiculo: `${vehiculo.marca} ${vehiculo.modelo}`.trim(),
        patente: vehiculo.patente,
    };
}

async function enrichReservations(reservas) {
    return Promise.all(reservas.map(enrichReservation));
}

const rootValue = {
    reservas: async ({ filtro }) => {
        const rentalFilter = await toRentalFilter(filtro);
        if (rentalFilter.vehiculo_ids && rentalFilter.vehiculo_ids.length === 0) {
            return [];
        }
        const response = await rentalService.listReservations(rentalFilter);
        return enrichReservations(response.reservas || []);
    },
    reserva: async ({ id }) => {
        return enrichReservation(await rentalService.getReservation(id));
    },
    historial: async ({ cliente_id }) => {
        const response = await rentalService.getRentalHistory(cliente_id);
        return enrichReservations(response.reservas || []);
    },
    crearReserva: async ({ input }) => {
        return enrichReservation(await rentalService.createReservation(input));
    },
    cancelarReserva: async ({ id }) => {
        return enrichReservation(await rentalService.cancelReservation(id));
    }
};

module.exports = { schemaString, rootValue };
