const grpc = require('@grpc/grpc-js');
const protoLoader = require('@grpc/proto-loader');
const path = require('path');

// Función genérica para cargar cualquier contrato .proto
function createGrpcClient(protoFileName, serviceName, port) {
    const PROTO_PATH = path.join(__dirname, `../../proto/${protoFileName}`);
    
    const packageDefinition = protoLoader.loadSync(PROTO_PATH, {
        keepCase: true,
        longs: String,
        enums: String,
        defaults: true,
        oneofs: true
    });

    const protoDescriptor = grpc.loadPackageDefinition(packageDefinition);
    
    // Obtenemos el paquete (asumimos que el nombre del paquete coincide con el archivo sin extensión)
    const packageName = protoFileName.replace('.proto', '');
    const ServiceClass = protoDescriptor[packageName][serviceName];

    return new ServiceClass(
        `localhost:${port}`,
        grpc.credentials.createInsecure()
    );
}

module.exports = { createGrpcClient };