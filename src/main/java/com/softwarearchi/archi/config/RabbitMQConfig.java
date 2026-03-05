package com.softwarearchi.archi.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Configuration RabbitMQ pour l'architecture Event-Driven.
 * Définit les exchanges, queues et bindings pour la messagerie asynchrone.
 */
@Configuration
public class RabbitMQConfig {

    @Value("${app.mq.exchange}")
    private String exchange;

    @Value("${app.mq.rk.userRegistered}")
    private String userRegisteredRoutingKey;

    @Value("${app.mq.rk.emailVerified}")
    private String emailVerifiedRoutingKey;

    // Exchange principal pour les événements d'authentification 
    @Bean
    public TopicExchange authExchange() {
        return new TopicExchange(exchange);
    }

    // Queue des inscriptions avec redirection vers DLQ en cas d'échec 
    @Bean
    public Queue userRegisteredQueue() {
        return QueueBuilder.durable("notification.user-registered")
                .withArgument("x-dead-letter-exchange", "auth.events.dlq")
                .withArgument("x-dead-letter-routing-key", "dead-letter")
                .build();
    }

    // Exchange pour les Dead Letter Queues (messages en échec) 
    @Bean
    public TopicExchange dlqExchange() {
        return new TopicExchange("auth.events.dlq");
    }

    // Queue de stockage des messages échoués pour débogage 
    @Bean
    public Queue dlqQueue() {
        return QueueBuilder.durable("notification.user-registered.dlq").build();
    }

    // Liaison entre la DLQ et son exchange avec la routing key "dead-letter" 
    @Bean
    public Binding dlqBinding(Queue dlqQueue, TopicExchange dlqExchange) {
        return BindingBuilder.bind(dlqQueue).to(dlqExchange).with("dead-letter");
    }

    // Liaison entre la queue d'inscription et l'exchange principal 
    @Bean
    public Binding userRegisteredBinding(Queue userRegisteredQueue, TopicExchange authExchange) {
        return BindingBuilder.bind(userRegisteredQueue).to(authExchange).with(userRegisteredRoutingKey);
    }

    /**
     * Convertisseur de messages JSON utilisant Jackson.
     * Le module JavaTimeModule permet la sérialisation correcte des types Java 8 Date/Time
     * (LocalDateTime, Instant, etc.) dans les messages RabbitMQ.
     */
    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return new Jackson2JsonMessageConverter(mapper);
    }

    /**
     * Template RabbitMQ préconfiguré pour l'envoi de messages.
     * Utilise le convertisseur JSON pour automatiquement sérialiser
     * les objets Java en JSON avant envoi.
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }
}
