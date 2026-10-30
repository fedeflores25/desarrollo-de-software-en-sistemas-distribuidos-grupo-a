const amqp = require('amqplib');

// cred por defecto
const RABBITMQ_URL = process.env.RABBITMQ_URL || 'amqp://guest:guest@localhost:5672';
const EXCHANGE_NAME = 'rentar.eventos'; 

let channel = null;

async function connectRabbitMQ() {
    try {
        // 1. conectar al servidor RabbitMQ
        const connection = await amqp.connect(RABBITMQ_URL);
        
        // 2. crear un canal de comunicación
        channel = await connection.createChannel();
        
        // 3. Declarar el Exchange de tipo 'topic' (si no existe, lo crea)
        await channel.assertExchange(EXCHANGE_NAME, 'topic', { durable: true });
        
        console.log('Conectado exitosamente a RabbitMQ (Exchange: rentar.eventos)');
        return channel;
    } catch (error) {
        console.error('❌ Error al conectar a RabbitMQ:', error.message);
        // Si falla, reintenta en 5 segundos
        setTimeout(connectRabbitMQ, 5000);
    }
}

// Función para exportar el canal y usarlo en otros archivos
function getChannel() {
    if (!channel) {
        throw new Error('El canal de RabbitMQ aún no está inicializado');
    }
    return channel;
}

module.exports = {
    connectRabbitMQ,
    getChannel,
    EXCHANGE_NAME
};