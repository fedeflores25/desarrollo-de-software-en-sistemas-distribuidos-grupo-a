package com.rentar.titularidad.event;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Configuracion RabbitMQ del registro de titularidad. */
@Configuration
public class EventosConfig {

    public static final String EXCHANGE = "rentar.eventos";

    public static final String EXTERNO_REGISTRADO =
            "vehiculos.externos.registrados";

    public static final String VALIDACION_TITULARIDAD =
            "vehiculos.externos.validacion.titularidad";

    public static final String COLA_EXTERNO_REGISTRADO =
            "titularidad.vehiculos.externos.registrados";

    @Bean
    TopicExchange rentarEventosExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    Queue externoRegistradoQueue() {
        return new Queue(COLA_EXTERNO_REGISTRADO, true);
    }

    @Bean
    Binding externoRegistradoBinding(
            Queue externoRegistradoQueue,
            TopicExchange rentarEventosExchange) {

        return BindingBuilder.bind(externoRegistradoQueue)
                .to(rentarEventosExchange)
                .with(EXTERNO_REGISTRADO);
    }
}