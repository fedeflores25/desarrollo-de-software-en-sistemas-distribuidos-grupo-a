const { rentalClient } = require('../grpc-clients/rentalClient');

// Se importan los servicios necesarios para la orquestacion.
const customerService = require('./customerService');
const vehicleService = require('./vehicleService');

// Se definen los valores utilizados por gRPC.
const ESTADOS_GRPC = {
    CONFIRMADA: 'RESERVATION_STATUS_CONFIRMADA',
    CANCELADA: 'RESERVATION_STATUS_CANCELADA',
    FINALIZADA: 'RESERVATION_STATUS_FINALIZADA'
};

// Se definen los estados que se devuelven a REST y GraphQL.
const ESTADOS_PUBLICOS = {
    RESERVATION_STATUS_CONFIRMADA: 'CONFIRMADA',
    RESERVATION_STATUS_CANCELADA: 'CANCELADA',
    RESERVATION_STATUS_FINALIZADA: 'FINALIZADA'
};

const MANTENIMIENTOS_PUBLICOS = {
    MAINTENANCE_STATUS_PENDIENTE: 'PENDIENTE',
    MAINTENANCE_STATUS_REALIZADO: 'REALIZADO',
    MAINTENANCE_STATUS_NO_REALIZADO: 'NO_REALIZADO'
};

// Se unifica la invocacion de los metodos gRPC.
const callGrpc = (methodName, requestData) => {
    return new Promise((resolve, reject) => {
        rentalClient[methodName](requestData, (error, response) => {
            if (error) reject(error);
            else resolve(response);
        });
    });
};

// Se normalizan los estados recibidos desde Rental Service.
function normalizarReserva(reserva) {
    if (!reserva) {
        return reserva;
    }

    return {
        ...reserva,
        estado: ESTADOS_PUBLICOS[reserva.estado] || reserva.estado,
        mantenimiento: MANTENIMIENTOS_PUBLICOS[reserva.mantenimiento]
            || reserva.mantenimiento
    };
}

// Se normalizan las reservas de una respuesta gRPC.
function normalizarListaReservas(response) {
    return {
        ...response,
        reservas: (response.reservas || []).map(normalizarReserva)
    };
}

// Se verifica el cliente y el vehiculo antes de crear la reserva.
async function createReservation(data) {
    try {
        const customerOk = await customerService.verifyCustomer(data.cliente_id);

        if (!customerOk.existe || !customerOk.activo) {
            throw new Error('El cliente no existe o se encuentra inactivo');
        }

        const vehicle = await vehicleService.getVehicle(data.vehiculo_id);

        if (!vehicle.activo || vehicle.estado !== 'DISPONIBLE') {
            throw new Error('El vehículo no está disponible para alquilar');
        }

        // Se prepara la solicitud con el precio diario del vehiculo.
        const grpcRequest = {
            cliente_id: data.cliente_id,
            vehiculo_id: data.vehiculo_id,
            fecha_inicio: data.fecha_inicio,
            fecha_fin: data.fecha_fin,
            precio_diario: vehicle.precio_diario
        };

        // Se solicita la creacion de la reserva al Rental Service.
        const response = await callGrpc('CreateReservation', grpcRequest);
        return normalizarReserva(response);

    } catch (error) {
        console.error('Error orquestando reserva:', error);
        throw error;
    }
}

// Se consulta la disponibilidad de vehiculos para el periodo indicado.
async function checkAvailability(filtro) {
    const { fecha_inicio, fecha_fin, ...vehicleFilter } = filtro;

    const ocupados = await callGrpc('GetBusyVehicleIds', {
        fecha_inicio,
        fecha_fin
    });

    const blacklist = ocupados.vehiculo_ids || [];

    return await vehicleService.listAvailableVehicles(
        vehicleFilter,
        blacklist
    );
}

// Se cancela una reserva y se normaliza su estado.
async function cancelReservation(id) {
    const response = await callGrpc('CancelReservation', { id });
    return normalizarReserva(response);
}

// Se consulta una reserva por su identificador.
async function getReservation(id) {
    const response = await callGrpc('GetReservation', { id });
    return normalizarReserva(response);
}

// Se obtiene el historial de reservas de un cliente.
async function getRentalHistory(cliente_id) {
    const response = await callGrpc('GetRentalHistory', { cliente_id });
    return normalizarListaReservas(response);
}

// Se convierten los filtros y se consulta el listado de reservas.
async function listReservations(filtro = {}) {
    const grpcFilter = { ...filtro };

    if (grpcFilter.estado && ESTADOS_GRPC[grpcFilter.estado]) {
        grpcFilter.estado = ESTADOS_GRPC[grpcFilter.estado];
    }

    const response = await callGrpc('ListReservations', grpcFilter);
    return normalizarListaReservas(response);
}

module.exports = {
    createReservation,
    checkAvailability,
    cancelReservation,
    getReservation,
    listReservations,
    getRentalHistory
};
