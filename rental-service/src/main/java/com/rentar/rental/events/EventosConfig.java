package com.rentar.rental.events;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Exchange, colas y bindings del Rental Service (ver docs/eventos.md).
 *
 * Rental tambien declara la cola del Maintenance Service: si ese servicio todavia no
 * arranco, los eventos reserva.confirmada quedan guardados en la cola en lugar de perderse.
 * Las dos puntas declaran la cola igual (durable, sin argumentos), asi no hay conflicto.
 */
@Configuration
@EnableAsync
public class EventosConfig {

    public static final String EXCHANGE = "rentar.eventos";

    public static final String RK_RESERVA_CONFIRMADA = "reserva.confirmada";
    public static final String RK_MANTENIMIENTO_RESULTADO = "mantenimiento.resultado";

    public static final String COLA_RESULTADO = "rental.mantenimiento.resultado";
    public static final String COLA_MANTENIMIENTO = "maintenance.reserva.confirmada";

    @Bean
    public TopicExchange eventosExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue colaResultadoMantenimiento() {
        return QueueBuilder.durable(COLA_RESULTADO).build();
    }

    @Bean
    public Binding bindingResultadoMantenimiento(Queue colaResultadoMantenimiento, TopicExchange eventosExchange) {
        return BindingBuilder.bind(colaResultadoMantenimiento).to(eventosExchange).with(RK_MANTENIMIENTO_RESULTADO);
    }

    @Bean
    public Queue colaSolicitudMantenimiento() {
        return QueueBuilder.durable(COLA_MANTENIMIENTO).build();
    }

    @Bean
    public Binding bindingSolicitudMantenimiento(Queue colaSolicitudMantenimiento, TopicExchange eventosExchange) {
        return BindingBuilder.bind(colaSolicitudMantenimiento).to(eventosExchange).with(RK_RESERVA_CONFIRMADA);
    }
}
