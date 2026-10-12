const grpc = require('@grpc/grpc-js');
const protoLoader = require('@grpc/proto-loader');
const path = require('path');

const PROTO_PATH = path.join(__dirname, '../../../proto/rental.proto');

const packageDefinition = protoLoader.loadSync(PROTO_PATH, {
    keepCase: true,
    longs: String,
    enums: String,
    defaults: true,
    oneofs: true
});

const rentalProto = grpc.loadPackageDefinition(packageDefinition).rentar.rental.v1;

const rentalClient = new rentalProto.RentalService(
    'localhost:9093',
    grpc.credentials.createInsecure()
);

module.exports = { rentalClient };