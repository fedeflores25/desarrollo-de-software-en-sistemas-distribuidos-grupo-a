package com.rentar.vehicle.events;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/** Exchange y colas durables utilizados por Vehicle Service en el Hito 3. */
@Configuration
@EnableAsync
public class EventosConfig {

    public static final String EXCHANGE = "rentar.eventos";

    // Verificacion de vehiculos normales
    public static final String RK_VERIFICACION_SOLICITADA =
            "vehiculos.verificacion.solicitada";

    public static final String RK_VERIFICACION_RESULTADO =
            "vehiculos.verificacion.resultado";

    public static final String COLA_RESULTADO =
            "vehicle.vehiculos.verificacion.resultado";

    // Incorporacion de vehiculos externos
    public static final String RK_EXTERNO_REGISTRADO =
            "vehiculos.externos.registrados";

    public static final String RK_EXTERNO_VALIDACION_PATENTE =
            "vehiculos.externos.validacion.patente";

    public static final String RK_EXTERNO_VALIDACION_TITULARIDAD =
            "vehiculos.externos.validacion.titularidad";

    public static final String RK_EXTERNO_VALIDACION_COMPLETADA =
            "vehiculos.externos.validacion.completada";

    // Colas para las validaciones externas
    public static final String COLA_EXTERNO_PATENTE =
            "incorporacion.vehiculos.externos.validacion.patente";

    public static final String COLA_EXTERNO_TITULARIDAD =
            "incorporacion.vehiculos.externos.validacion.titularidad";

    public static final String COLA_EXTERNO_COMPLETADA =
            "vehicle.vehiculos.externos.validacion.completada";

    @Bean
    TopicExchange eventosExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    // Cola de verificacion de vehiculos normales
    @Bean
    Queue colaResultadoVerificacion() {
        return QueueBuilder.durable(COLA_RESULTADO).build();
    }

    @Bean
    Binding bindingResultadoVerificacion(
            Queue colaResultadoVerificacion,
            TopicExchange eventosExchange) {

        return BindingBuilder.bind(colaResultadoVerificacion)
                .to(eventosExchange)
                .with(RK_VERIFICACION_RESULTADO);
    }

    // Cola de validacion de patente externa
    @Bean
    Queue colaExternoPatente() {
        return QueueBuilder.durable(COLA_EXTERNO_PATENTE).build();
    }

    @Bean
    Binding bindingExternoPatente(
            Queue colaExternoPatente,
            TopicExchange eventosExchange) {

        return BindingBuilder.bind(colaExternoPatente)
                .to(eventosExchange)
                .with(RK_EXTERNO_VALIDACION_PATENTE);
    }

    // Cola de validacion de titularidad externa
    @Bean
    Queue colaExternoTitularidad() {
        return QueueBuilder.durable(COLA_EXTERNO_TITULARIDAD).build();
    }

    @Bean
    Binding bindingExternoTitularidad(
            Queue colaExternoTitularidad,
            TopicExchange eventosExchange) {

        return BindingBuilder.bind(colaExternoTitularidad)
                .to(eventosExchange)
                .with(RK_EXTERNO_VALIDACION_TITULARIDAD);
    }

    // Cola de incorporacion completada
    @Bean
    Queue colaExternoCompletada() {
        return QueueBuilder.durable(COLA_EXTERNO_COMPLETADA).build();
    }

    @Bean
    Binding bindingExternoCompletada(
            Queue colaExternoCompletada,
            TopicExchange eventosExchange) {

        return BindingBuilder.bind(colaExternoCompletada)
                .to(eventosExchange)
                .with(RK_EXTERNO_VALIDACION_COMPLETADA);
    }
}