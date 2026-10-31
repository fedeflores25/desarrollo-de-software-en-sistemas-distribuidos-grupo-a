const { customerClient } = require('../grpc-clients/customerClient');


// 1. Listar clientes (soporta filtrar por activos o por IDs)
async function listCustomers(ids = [], soloActivos = null) {
    return new Promise((resolve, reject) => {
        const request = { ids, solo_activos: soloActivos };
        customerClient.ListCustomers(request, (error, response) => {
            if (error) reject(error);
            else resolve(response.clientes || []);
        });
    });
}


// 2. Obtener un cliente específico (Devuelve NOT_FOUND si no existe)
async function getCustomer(id) {
    return new Promise((resolve, reject) => {
        customerClient.GetCustomer({ id }, (error, response) => {
            if (error) reject(error);
            else resolve(response);
        });
    });
}

// 3. Crear cliente
async function createCustomer(data) {
    return new Promise((resolve, reject) => {
        // data debe traer: documento, nombre, apellido, email, telefono, fecha_nacimiento
        customerClient.CreateCustomer(data, (error, response) => {
            if (error) reject(error);
            else resolve(response);
        });
    });
}

// 4. Actualizar cliente
async function updateCustomer(data) {
    return new Promise((resolve, reject) => {
        // data debe traer id y el resto de los campos
        customerClient.UpdateCustomer(data, (error, response) => {
            if (error) reject(error);
            else resolve(response);
        });
    });
}

// 5. Baja lógica
async function deactivateCustomer(id) {
    return new Promise((resolve, reject) => {
        customerClient.DeactivateCustomer({ id }, (error, response) => {
            if (error) reject(error);
            else resolve(response);
        });
    });
}

// 6. Verificar cliente (Usado para el flujo de Reservas)
async function verifyCustomer(id) {
    return new Promise((resolve, reject) => {
        customerClient.VerifyCustomer({ id }, (error, response) => {
            if (error) reject(error);
            else resolve(response);
        });
    });
}

// 7. Autenticar un usuario para iniciar sesion en el Gateway.
async function authenticateUser(username, password) {
    return new Promise((resolve, reject) => {
        customerClient.AuthenticateUser({ username, password }, (error, response) => {
            if (error) reject(error);
            else resolve(response);
        });
    });
}

module.exports = {
    listCustomers,
    getCustomer,
    createCustomer,
    updateCustomer,
    deactivateCustomer,
    verifyCustomer,
    authenticateUser
};
