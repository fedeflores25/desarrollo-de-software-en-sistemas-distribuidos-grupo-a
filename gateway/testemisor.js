const amqp = require('amqplib');
const { EXCHANGE_NAME } = require('./src/config/rabbitmq');

async function simular() {
    try {
        const connection = await amqp.connect('amqp://guest:guest@localhost:5672');
        const channel = await connection.createChannel();
        
        // Simulamos la estructura exacta 
        const mensajePrueba = {
            correlationId: require('crypto').randomUUID(),
            payload: { 
                vehiculoId: 99, 
                patente: "ROB123" // Usa una patente robada
            }
        };

        // Publicamos al exchange con la etiqueta de solicitud
        channel.publish(
            EXCHANGE_NAME, 
            'vehiculos.verificacion.solicitada', 
            Buffer.from(JSON.stringify(mensajePrueba))
        );

        console.log("🚀 Mensaje de prueba enviado a RabbitMQ!");
        setTimeout(() => process.exit(0), 500); // Cerramos el script
    } catch (error) {
        console.error(error);
    }
}

simular();