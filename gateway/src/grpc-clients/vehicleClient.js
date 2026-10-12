const grpc = require('@grpc/grpc-js');
const protoLoader = require('@grpc/proto-loader');
const path = require('path');

const PROTO_PATH = path.join(__dirname, '../../../proto/vehicle.proto');

const packageDefinition = protoLoader.loadSync(PROTO_PATH, {
    keepCase: true,
    longs: String,
    enums: String,
    defaults: true,
    oneofs: true
});

// Extraemos el paquete definido en el .proto
const vehicleProto = grpc.loadPackageDefinition(packageDefinition).rentar.vehicle.v1;

// Creamos el cliente apuntando al puerto (9091)
const vehicleClient = new vehicleProto.VehicleService(
    'localhost:9091',
    grpc.credentials.createInsecure()
);

module.exports = { vehicleClient };