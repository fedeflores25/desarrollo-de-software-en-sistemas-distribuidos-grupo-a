const { vehicleClient } = require('../grpc-clients/vehicleClient');

// Función auxiliar para convertir cualquier método gRPC en Promesa
const callGrpc = (methodName, requestData) => {
    return new Promise((resolve, reject) => {
        vehicleClient[methodName](requestData, (error, response) => {
            if (error) reject(error);
            else resolve(response);
        });
    });
};

// 1. Listar vehículos (con filtros opcionales)
async function listVehicles(filtro = {}, ids = [], soloActivos = null) {
    const response = await callGrpc('ListVehicles', { filtro, ids, solo_activos: soloActivos });
    return response.vehiculos || [];
}


// 2. Obtener un vehículo por ID
async function getVehicle(id) {
    return callGrpc('GetVehicle', { id });
}

// 3. Crear vehículo
async function createVehicle(data) {
    return callGrpc('CreateVehicle', data);
}

// 4. Modificar vehículo (sin patente, según el .proto)
async function updateVehicle(data) {
    return callGrpc('UpdateVehicle', data);
}

// 5. Baja lógica
async function deactivateVehicle(id) {
    return callGrpc('DeactivateVehicle', { id });
}

// 6. Cambiar estado operativo
async function updateVehicleStatus(id, estado) {
    return callGrpc('UpdateVehicleStatus', { id, estado });
}

// 7. Listar disponibles (Usado en el flujo con RentalService)
async function listAvailableVehicles(filtro = {}, excluirIds = []) {
    const response = await callGrpc('ListAvailableVehicles', { filtro, excluir_ids: excluirIds });
    return response.vehiculos || [];
}

async function getVehicleByPatente(patente) {
    return callGrpc('GetVehicleByPatente', { patente });
}

module.exports = {
    listVehicles,
    getVehicle,
    getVehicleByPatente,
    createVehicle,
    updateVehicle,
    deactivateVehicle,
    updateVehicleStatus,
    listAvailableVehicles
};