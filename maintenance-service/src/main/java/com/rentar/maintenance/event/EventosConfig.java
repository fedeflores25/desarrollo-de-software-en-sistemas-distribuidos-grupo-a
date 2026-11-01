package com.rentar.maintenance.event;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Declaracion local de los recursos RabbitMQ que consume Maintenance Service. */
@Configuration
public class EventosConfig {

    public static final String EXCHANGE = "rentar.eventos";
    public static final String RESERVA_CONFIRMADA = "reserva.confirmada";
    public static final String MANTENIMIENTO_RESULTADO = "mantenimiento.resultado";
    public static final String COLA_RESERVA_CONFIRMADA = "maintenance.reserva.confirmada";

    @Bean
    TopicExchange rentarEventosExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    Queue reservaConfirmadaQueue() {
        return new Queue(COLA_RESERVA_CONFIRMADA, true);
    }

    @Bean
    Binding reservaConfirmadaBinding(Queue reservaConfirmadaQueue, TopicExchange rentarEventosExchange) {
        return BindingBuilder.bind(reservaConfirmadaQueue)
                .to(rentarEventosExchange)
                .with(RESERVA_CONFIRMADA);
    }
}
