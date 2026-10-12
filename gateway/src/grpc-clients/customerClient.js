const grpc = require('@grpc/grpc-js');
const protoLoader = require('@grpc/proto-loader');
const path = require('path');

// Apuntamos al .proto 
const PROTO_PATH = path.join(__dirname, '../../../proto/customer.proto');

const packageDefinition = protoLoader.loadSync(PROTO_PATH, {
    keepCase: true,
    longs: String,
    enums: String,
    defaults: true,
    oneofs: true
});

// Extraemos el paquete en base al .proto 
const customerProto = grpc.loadPackageDefinition(packageDefinition).rentar.customer.v1;

// Creamos la conexión apuntando al puerto 9092
const customerClient = new customerProto.CustomerService(
    'localhost:9092',
    grpc.credentials.createInsecure()
);

module.exports = { customerClient };