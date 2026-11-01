const vehicleService = require('../../services/vehicleService');

// Se importa Rental Service para gestionar la consulta de disponibilidad.
const rentalService = require('../../services/rentalService');

const schemaString = `
  """
  Tipos de vehículos disponibles en la flota.
  """
  enum TipoVehiculo {
    SEDAN
    SUV
    PICKUP
    COUPE
    HATCHBACK
  }

  """
  Representa un vehículo registrado en el sistema Rentar.
  """
  type Vehiculo {
    """Identificador único del vehículo."""
    id: ID!

    """Patente del vehículo."""
    patente: String

    """Marca del vehículo."""
    marca: String

    """Modelo del vehículo."""
    modelo: String

    """Año de fabricación."""
    anio: Int

    """Color del vehículo."""
    color: String

    """Tipo de vehículo."""
    tipo: TipoVehiculo

    """Precio de alquiler por día."""
    precio_diario: Float

    """Estado operativo del vehículo."""
    estado: String

    """Indica si el vehículo está activo en el sistema."""
    activo: Boolean
  }

  """
  Filtros opcionales para la búsqueda de vehículos.
  """
  input VehiculoFiltroInput {
    """Filtra por tipo de vehículo."""
    tipo: TipoVehiculo

    """Filtra por marca."""
    marca: String

    """Filtra por modelo."""
    modelo: String

    """Precio diario mínimo."""
    precio_min: Float

    """Precio diario máximo."""
    precio_max: Float
  }

  """
  Período obligatorio y filtros opcionales para
  consultar la disponibilidad de vehículos.
  Las fechas deben incluir segundos y no admiten zona horaria.
  """
  input DisponibilidadFiltroInput {
    """
    Fecha y hora de inicio del período.
    Formato obligatorio YYYY-MM-DDTHH:mm:ss, sin zona horaria.
    No se admite el sufijo Z.
    """
    fecha_inicio: String!

    """
    Fecha y hora de fin del período, posterior al inicio.
    Formato obligatorio YYYY-MM-DDTHH:mm:ss, sin zona horaria.
    No se admite el sufijo Z.
    """
    fecha_fin: String!

    """Tipo de vehículo solicitado."""
    tipo: TipoVehiculo

    """Marca del vehículo."""
    marca: String

    """Modelo del vehículo."""
    modelo: String

    """Precio diario mínimo."""
    precio_min: Float

    """Precio diario máximo."""
    precio_max: Float
  }
`;

const rootValue = {
  vehiculos: async ({ filtro, soloActivos }) =>
    await vehicleService.listVehicles(filtro || {}, [], soloActivos),

  vehiculo: async ({ id }) =>
    await vehicleService.getVehicle(id),

  // Se consulta la disponibilidad de vehiculos mediante Rental Service.
  vehiculosDisponibles: async ({ filtro }) => {
    return await rentalService.checkAvailability(filtro);
  }
};

module.exports = { schemaString, rootValue };
