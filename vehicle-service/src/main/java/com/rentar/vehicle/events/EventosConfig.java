package com.rentar.vehicle.events;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/** Exchange y cola durable utilizados por Vehicle Service en el Hito 3. */
@Configuration
@EnableAsync
public class EventosConfig {

    public static final String EXCHANGE = "rentar.eventos";
    public static final String RK_VERIFICACION_SOLICITADA = "vehiculos.verificacion.solicitada";
    public static final String RK_VERIFICACION_RESULTADO = "vehiculos.verificacion.resultado";
    public static final String COLA_RESULTADO = "vehicle.vehiculos.verificacion.resultado";

    @Bean
    TopicExchange eventosExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    Queue colaResultadoVerificacion() {
        return QueueBuilder.durable(COLA_RESULTADO).build();
    }

    @Bean
    Binding bindingResultadoVerificacion(Queue colaResultadoVerificacion, TopicExchange eventosExchange) {
        return BindingBuilder.bind(colaResultadoVerificacion)
                .to(eventosExchange)
                .with(RK_VERIFICACION_RESULTADO);
    }
}
