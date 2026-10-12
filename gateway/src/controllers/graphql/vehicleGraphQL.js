const vehicleService = require('../../services/vehicleService');
// Importamos rentalService por la orquestación de disponibilidad real
const rentalService = require('../../services/rentalService'); 

const schemaString = `
  """Tipos de vehículos disponibles en la flota"""
  enum TipoVehiculo {
    SEDAN
    SUV
    PICKUP
    COUPE
    HATCHBACK
  }

  type Vehiculo {
    id: ID!
    patente: String
    marca: String
    modelo: String
    anio: Int
    color: String
    tipo: TipoVehiculo
    precio_diario: Float
    estado: String
    activo: Boolean
  }

  """Filtros opcionales para la búsqueda de vehículos"""
  input VehiculoFiltroInput {
    tipo: TipoVehiculo
    marca: String
    modelo: String
    precio_min: Float
    precio_max: Float
  }
`;

const rootValue = {
    vehiculos: async ({ filtro, soloActivos }) => await vehicleService.listVehicles(filtro || {}, [], soloActivos),
    vehiculo: async ({ id }) => await vehicleService.getVehicle(id),
    
    // Acá conectamos la orquestación real que armamos antes
    vehiculosDisponibles: async ({ filtro }) => {
        if (!filtro || !filtro.fecha_inicio || !filtro.fecha_fin) {
            throw new Error("Las fechas de inicio y fin son obligatorias para la disponibilidad");
        }
        return await rentalService.checkAvailability(filtro);
    }
};

module.exports = { schemaString, rootValue };