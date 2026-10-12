const { rentalClient } = require('../grpc-clients/rentalClient');
// IMPORTAMOS LOS OTROS SERVICIOS PARA LA ORQUESTACIÓN
const customerService = require('./customerService');
const vehicleService = require('./vehicleService');

const callGrpc = (methodName, requestData) => {
    return new Promise((resolve, reject) => {
        rentalClient[methodName](requestData, (error, response) => {
            if (error) reject(error);
            else resolve(response);
        });
    });
};

// --- EL FLUJO COMPLEJO (CREAR RESERVA) ---
async function createReservation(data) {
    try {
        // 1. Gateway -> CustomerService.VerifyCustomer (existe y esta activo)
        const customerOk = await customerService.verifyCustomer(data.cliente_id);
        if (!customerOk.existe || !customerOk.activo) {
            throw new Error('El cliente no existe o se encuentra inactivo');
        }

        // 2. Gateway -> VehicleService.GetVehicle (existe, esta activo, precio diario)
        const vehicle = await vehicleService.getVehicle(data.vehiculo_id);
        if (!vehicle.activo || vehicle.estado !== 'DISPONIBLE') {
            throw new Error('El vehículo no está disponible para alquilar');
        }

        // Armamos el objeto para mandarle a (RentalService Java)
        // inyectamos el precio_diario (VehicleService)
        const grpcRequest = {
            cliente_id: data.cliente_id,
            vehiculo_id: data.vehiculo_id,
            fecha_inicio: data.fecha_inicio,
            fecha_fin: data.fecha_fin,
            precio_diario: vehicle.precio_diario
        };

        // 3. Gateway -> RentalService.CreateReservation (valida fechas y solapamiento, calcula importe)
        return await callGrpc('CreateReservation', grpcRequest);

    } catch (error) {
        // Si cualquier servicio interno falla, el Gateway atrapa el error y aborta todo
        console.error("Error orquestando reserva:", error);
        throw error;
    }
}

// --- EL FLUJO DE DISPONIBILIDAD 
async function checkAvailability(filtro) {
    // 1. Gateway -> RentalService.GetBusyVehicleIds
    const ocupados = await callGrpc('GetBusyVehicleIds', { 
        fecha_inicio: filtro.fecha_inicio, 
        fecha_fin: filtro.fecha_fin 
    });
    
    const blacklist = ocupados.vehiculo_ids || [];

    // 2. Gateway -> VehicleService.ListAvailableVehicles (pasándole la lista negra)
    return await vehicleService.listAvailableVehicles(filtro, blacklist);
}

// --- OTROS MÉTODOS LINEALES ---
async function cancelReservation(id) { return callGrpc('CancelReservation', { id }); }
async function getReservation(id) { return callGrpc('GetReservation', { id }); }
async function getRentalHistory(cliente_id) { return callGrpc('GetRentalHistory', { cliente_id }); }

async function listReservations(filtro) { return callGrpc('ListReservations', filtro); }


module.exports = {
    createReservation,
    checkAvailability,
    cancelReservation,
    getReservation,
    listReservations,
    getRentalHistory
};