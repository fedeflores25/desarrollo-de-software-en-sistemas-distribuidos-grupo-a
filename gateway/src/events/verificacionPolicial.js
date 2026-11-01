const { getChannel, EXCHANGE_NAME } = require('../config/rabbitmq');
const crypto = require('crypto');

// 1. Importar la bd simulada
const patentesRobadas = require('../../patentesRobadas.json');

// Genera la fecha local sin zona horaria y con segundos.
function fechaEvento() {
    const ahora = new Date();
    const dosDigitos = (valor) => String(valor).padStart(2, '0');

    const fecha = [
        ahora.getFullYear(),
        dosDigitos(ahora.getMonth() + 1),
        dosDigitos(ahora.getDate())
    ].join('-');

    const hora = [
        dosDigitos(ahora.getHours()),
        dosDigitos(ahora.getMinutes()),
        dosDigitos(ahora.getSeconds())
    ].join(':');

    return `${fecha}T${hora}`;
}

async function iniciarRegistroPolicial() {
    const channel = getChannel();
    const queueName = 'gateway.registro_policial.queue';

    // Etiquetas acordadas para el consumo y publicacion de eventos.
    const consumeRoutingKey = 'vehiculos.verificacion.solicitada';
    const publishRoutingKey = 'vehiculos.verificacion.resultado';

    // Declarar la cola donde el Gateway recibe los mensajes.
    await channel.assertQueue(queueName, { durable: true });

    // Unir la cola al exchange.
    await channel.bindQueue(queueName, EXCHANGE_NAME, consumeRoutingKey);

    console.log(`Registro Policial escuchando eventos: ${consumeRoutingKey}`);

    channel.consume(queueName, (msg) => {
        if (msg !== null) {
            const contenido = JSON.parse(msg.content.toString());
            const payload = contenido.payload;

            console.log(`\n Verificando patente: ${payload.patente}`);

            const esRobado = patentesRobadas.includes(payload.patente);
            const resultadoFinal = esRobado ? 'ROBADO' : 'VALIDADO';

            console.log(`Resultado determinado: ${resultadoFinal}`);

            const respuestaEvento = {
                eventId: crypto.randomUUID(),
                eventType: publishRoutingKey,
                correlationId: contenido.correlationId || crypto.randomUUID(),
                occurredAt: fechaEvento(),
                payload: {
                    vehiculoId: payload.vehiculoId,
                    patente: payload.patente,
                    resultado: resultadoFinal
                }
            };

            channel.publish(
                EXCHANGE_NAME,
                publishRoutingKey,
                Buffer.from(JSON.stringify(respuestaEvento)),
                { persistent: true }
            );

            channel.ack(msg);
        }
    });

    const externalQueue = 'gateway.registro_policial.externos.queue';
    const externalRequest = 'vehiculos.externos.registrados';
    const externalResponse = 'vehiculos.externos.validacion.patente';

    await channel.assertQueue(externalQueue, { durable: true });
    await channel.bindQueue(externalQueue, EXCHANGE_NAME, externalRequest);

    console.log(`Registro Policial escuchando eventos: ${externalRequest}`);

    channel.consume(externalQueue, (msg) => {
        if (msg === null) return;

        try {
            const contenido = JSON.parse(msg.content.toString());
            const payload = contenido.payload || {};

            if (!payload.vehiculoExternoId || !payload.patente) {
                throw new Error('Evento de vehiculo externo incompleto');
            }

            const resultado = patentesRobadas.includes(payload.patente)
                ? 'RECHAZADO'
                : 'VALIDADO';

            const respuesta = {
                eventId: crypto.randomUUID(),
                eventType: externalResponse,
                correlationId: contenido.correlationId || crypto.randomUUID(),
                occurredAt: fechaEvento(),
                payload: {
                    vehiculoExternoId: payload.vehiculoExternoId,
                    patente: payload.patente,
                    resultado,
                },
            };

            channel.publish(
                EXCHANGE_NAME,
                externalResponse,
                Buffer.from(JSON.stringify(respuesta)),
                { persistent: true },
            );

            channel.ack(msg);
        } catch (error) {
            console.error('Evento policial externo invalido:', error.message);
            channel.nack(msg, false, false);
        }
    });
}

module.exports = { iniciarRegistroPolicial };
