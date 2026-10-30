const rentalService = require('../../services/rentalService');
const customerService = require('../../services/customerService');
const vehicleService = require('../../services/vehicleService');

const schemaString = `
  """
  Estados posibles de una reserva.
  """
  enum EstadoReserva {
    """La reserva se encuentra confirmada."""
    CONFIRMADA

    """La reserva fue cancelada."""
    CANCELADA

    """El período de alquiler ha finalizado."""
    FINALIZADA
  }

  """
  Representa una reserva registrada en el sistema Rentar.
  Contiene los datos del alquiler, su importe y estado.
  """
  type Reservation {
    """Identificador único de la reserva."""
    id: ID!

    """Identificador del cliente asociado."""
    cliente_id: ID

    """Identificador del vehículo reservado."""
    vehiculo_id: ID

    """
    Fecha y hora de inicio del alquiler.
    Formato ISO-8601 sin zona horaria:
    YYYY-MM-DDTHH:mm:ss.
    """
    fecha_inicio: String

    """
    Fecha y hora de finalización del alquiler.
    Formato ISO-8601 sin zona horaria:
    YYYY-MM-DDTHH:mm:ss.
    """
    fecha_fin: String

    """Precio diario aplicado al momento de reservar."""
    precio_diario: Float

    """Cantidad de días calculados para el alquiler."""
    cantidad_dias: Int

    """Importe total calculado para la reserva."""
    importe_total: Float

    """Estado actual de la reserva."""
    estado: EstadoReserva

    """Fecha y hora de registro. Formato YYYY-MM-DDTHH:mm:ss, sin zona horaria."""
    fecha_alta: String

    """
    Fecha y hora de cancelación.
    Formato YYYY-MM-DDTHH:mm:ss, sin zona horaria.
    Cadena vacía si la reserva no fue cancelada.
    """
    fecha_cancelacion: String

    """Nombre y apellido del cliente asociado."""
    cliente: String

    """Marca y modelo del vehículo reservado."""
    vehiculo: String

    """Patente del vehículo asociado a la reserva."""
    patente: String
    
    """Resultado del mantenimiento del vehículo."""
    mantenimiento_resultado: String # Puede ser "REALIZADO" o "NO_REALIZADO"
  }

  """
  Filtros opcionales para consultar reservas.
  Se pueden combinar para limitar los resultados.
  """
  input ReservaFiltroInput {
    """Filtra las reservas por cliente."""
    cliente_id: ID

    """Filtra las reservas por vehículo."""
    vehiculo_id: ID

    """Filtra según el tipo de vehículo."""
    tipo_vehiculo: TipoVehiculo

    """Filtra según el estado de la reserva."""
    estado: EstadoReserva

    """
    Fecha inicial del rango de consulta.
    Formato YYYY-MM-DDTHH:mm:ss, sin zona horaria.
    """
    desde: String

    """
    Fecha final del rango de consulta.
    Formato YYYY-MM-DDTHH:mm:ss, sin zona horaria.
    """
    hasta: String
  }

  """
  Datos necesarios para crear una reserva.
  Se requiere un cliente, un vehículo y un período válido.
  """
  input CreateReservationInput {
    """Identificador del cliente que realiza la reserva."""
    cliente_id: ID!

    """Identificador del vehículo solicitado."""
    vehiculo_id: ID!

    """
    Fecha y hora de inicio del alquiler.
    Formato ISO-8601 sin zona horaria:
    YYYY-MM-DDTHH:mm:ss.
    """
    fecha_inicio: String!

    """
    Fecha y hora de finalización del alquiler.
    Debe ser posterior a la fecha de inicio.
    Formato ISO-8601 sin zona horaria:
    YYYY-MM-DDTHH:mm:ss.
    """
    fecha_fin: String!
  }
`;

// Se preparan los filtros para consultar Rental Service.
async function toRentalFilter(filtro) {
  const { vehiculo_id, tipo_vehiculo, ...rentalFilter } = filtro || {};
  let vehicleIds;

  if (vehiculo_id) {
    vehicleIds = new Set([String(vehiculo_id)]);
  }

  if (tipo_vehiculo) {
    const vehicles = await vehicleService.listVehicles(
      { tipo: tipo_vehiculo }, [], true
    );

    const typeVehicleIds = new Set(
      vehicles.map((vehicle) => String(vehicle.id))
    );

    vehicleIds = vehicleIds
      ? new Set([...vehicleIds].filter((id) => typeVehicleIds.has(id)))
      : typeVehicleIds;
  }

  if (vehicleIds) {
    rentalFilter.vehiculo_ids = [...vehicleIds];
  }

  return rentalFilter;
}

// Se completa la informacion de la reserva con los datos
// del cliente y del vehiculo asociados.
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

// Se completa la informacion de cada reserva de la lista.
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
