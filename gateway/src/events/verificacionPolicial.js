const { getChannel, EXCHANGE_NAME } = require('../config/rabbitmq');
const crypto = require('crypto'); // Nativo de Node.js para generar UUIDs

// 1. Importar la bd simulada
const patentesRobadas = require('../../patentesRobadas.json');

async function iniciarRegistroPolicial() {
    const channel = getChannel();
    const queueName = 'gateway.registro_policial.queue';
    // etiquetas acordadas para el consumo y publicación de eventos 
    const consumeRoutingKey = 'vehiculos.verificacion.solicitada';
    const publishRoutingKey = 'vehiculos.verificacion.resultado';

    // Declarar la cola donde el Gateway va a recibir los mensajes
    await channel.assertQueue(queueName, { durable: true });
    // Unir la cola al Exchange filtrando solo por la solicitud de verificación
    await channel.bindQueue(queueName, EXCHANGE_NAME, consumeRoutingKey);

    console.log(`Registro Policial escuchando eventos: ${consumeRoutingKey}`);
    // Comenzar a consumir mensajes de la cola
    channel.consume(queueName, (msg) => {
        if (msg !== null) {
            const contenido = JSON.parse(msg.content.toString());
            const payload = contenido.payload;

            console.log(`\n Verificando patente: ${payload.patente}`);

            //  Logica simulada-> Si la patente está en la lista de patentes robadas, es robado.
            const esRobado = patentesRobadas.includes(payload.patente);
            const resultadoFinal = esRobado ? 'ROBADO' : 'VALIDADO';

            console.log(`Resultado determinado: ${resultadoFinal}`);

            // sobre de respuesta con la estructura definida en equipo
            const respuestaEvento = {
                eventId: crypto.randomUUID(),
                eventType: publishRoutingKey,
                correlationId: contenido.correlationId || crypto.randomUUID(),
                occurredAt: new Date().toISOString(),
                payload: {
                    vehiculoId: payload.vehiculoId,
                    patente: payload.patente,
                    resultado: resultadoFinal
                }
            };

            // Publicar la respuesta de vuelta al Exchange
            channel.publish(
                EXCHANGE_NAME,
                publishRoutingKey,
                Buffer.from(JSON.stringify(respuestaEvento))
            );

            // Confirmar a RabbitMQ que procesamos el mensaje para que lo borre de la cola
            channel.ack(msg);
        }
    });
}

module.exports = { iniciarRegistroPolicial };