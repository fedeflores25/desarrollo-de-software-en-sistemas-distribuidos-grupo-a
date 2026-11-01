const { vehicleClient } = require('../grpc-clients/vehicleClient');

// Se definen los nombres de los enumerados utilizados por gRPC.
const TIPOS_GRPC = {
  SEDAN: 'VEHICLE_TYPE_SEDAN',
  SUV: 'VEHICLE_TYPE_SUV',
  PICKUP: 'VEHICLE_TYPE_PICKUP',
  COUPE: 'VEHICLE_TYPE_COUPE',
  HATCHBACK: 'VEHICLE_TYPE_HATCHBACK'
};

const ESTADOS_GRPC = {
  DISPONIBLE: 'VEHICLE_STATUS_DISPONIBLE',
  RESERVADO: 'VEHICLE_STATUS_RESERVADO',
  EN_ALQUILER: 'VEHICLE_STATUS_EN_ALQUILER'
};

const TIPOS_PUBLICOS = {
  VEHICLE_TYPE_SEDAN: 'SEDAN',
  VEHICLE_TYPE_SUV: 'SUV',
  VEHICLE_TYPE_PICKUP: 'PICKUP',
  VEHICLE_TYPE_COUPE: 'COUPE',
  VEHICLE_TYPE_HATCHBACK: 'HATCHBACK'
};

const ESTADOS_PUBLICOS = {
  VEHICLE_STATUS_DISPONIBLE: 'DISPONIBLE',
  VEHICLE_STATUS_RESERVADO: 'RESERVADO',
  VEHICLE_STATUS_EN_ALQUILER: 'EN_ALQUILER'
};

// Se ejecutan las operaciones gRPC mediante promesas.
const callGrpc = (methodName, requestData) => {
  return new Promise((resolve, reject) => {
    vehicleClient[methodName](requestData, (error, response) => {
      if (error) reject(error);
      else resolve(response);
    });
  });
};

// Se adapta el tipo del filtro al contrato gRPC.
function prepararFiltro(filtro = {}) {
  return {
    ...filtro,
    ...(filtro.tipo != null
      ? { tipo: TIPOS_GRPC[filtro.tipo] || filtro.tipo }
      : {})
  };
}

// Se normaliza el vehiculo para REST, GraphQL y otros servicios del Gateway.
function normalizarVehiculo(vehiculo) {
  if (!vehiculo) return vehiculo;

  return {
    ...vehiculo,
    tipo: TIPOS_PUBLICOS[vehiculo.tipo] || vehiculo.tipo,
    estado: ESTADOS_PUBLICOS[vehiculo.estado] || vehiculo.estado
  };
}

// Se listan los vehiculos aplicando filtros opcionales.
async function listVehicles(filtro = {}, ids = [], soloActivos = null) {
  const response = await callGrpc('ListVehicles', {
    filtro: prepararFiltro(filtro),
    ids,
    solo_activos: soloActivos
  });

  return (response.vehiculos || []).map(normalizarVehiculo);
}

// Se consulta un vehiculo por su identificador.
async function getVehicle(id) {
  return normalizarVehiculo(await callGrpc('GetVehicle', { id }));
}

// Se consulta un vehiculo por su patente.
async function getVehicleByPatente(patente) {
  return normalizarVehiculo(
    await callGrpc('GetVehicleByPatente', { patente })
  );
}

// Se registra un nuevo vehiculo.
async function createVehicle(data) {
  const request = {
    ...data,
    tipo: TIPOS_GRPC[data.tipo] || data.tipo
  };

  return normalizarVehiculo(await callGrpc('CreateVehicle', request));
}

// Se actualizan los datos de un vehiculo.
async function updateVehicle(data) {
  const request = {
    ...data,
    tipo: TIPOS_GRPC[data.tipo] || data.tipo
  };

  return normalizarVehiculo(await callGrpc('UpdateVehicle', request));
}

// Se realiza la baja logica de un vehiculo.
async function deactivateVehicle(id) {
  return normalizarVehiculo(
    await callGrpc('DeactivateVehicle', { id })
  );
}

// Se modifica el estado operativo de un vehiculo.
async function updateVehicleStatus(id, estado) {
  const grpcEstado = ESTADOS_GRPC[estado] || estado;

  return normalizarVehiculo(
    await callGrpc('UpdateVehicleStatus', {
      id,
      estado: grpcEstado
    })
  );
}

// Se consultan los vehiculos disponibles.
async function listAvailableVehicles(filtro = {}, excluirIds = []) {
  const response = await callGrpc('ListAvailableVehicles', {
    filtro: prepararFiltro(filtro),
    excluir_ids: excluirIds
  });

  return (response.vehiculos || []).map(normalizarVehiculo);
}

async function createExternalVehicleApplication(data) {
  const request = {
    ...data,
    tipo: TIPOS_GRPC[data.tipo] || data.tipo
  };
  return callGrpc('CreateExternalVehicleApplication', request);
}

async function getExternalVehicleApplication(id) {
  return callGrpc('GetExternalVehicleApplication', { id });
}

module.exports = {
  listVehicles,
  getVehicle,
  getVehicleByPatente,
  createVehicle,
  updateVehicle,
  deactivateVehicle,
  updateVehicleStatus,
  listAvailableVehicles,
  createExternalVehicleApplication,
  getExternalVehicleApplication
};
