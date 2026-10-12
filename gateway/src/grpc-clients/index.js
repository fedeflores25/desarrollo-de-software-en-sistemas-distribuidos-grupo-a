const { createGrpcClient } = require('./clientFactory');

const vehicleClient = createGrpcClient('vehicle.proto', 'VehicleService', 9091);
const customerClient = createGrpcClient('customer.proto', 'CustomerService', 9092);
const rentalClient = createGrpcClient('rental.proto', 'RentalService', 9093);

module.exports = {
    vehicleClient,
    customerClient,
    rentalClient
};