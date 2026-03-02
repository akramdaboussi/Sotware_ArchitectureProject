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

@Configuration
public class RabbitMQConfig {

    @Value("${app.mq.exchange}")
    private String exchange;

    @Value("${app.mq.rk.userRegistered}")
    private String userRegisteredRoutingKey;

    @Value("${app.mq.rk.emailVerified}")
    private String emailVerifiedRoutingKey;

    @Bean
    public TopicExchange authExchange() {
        return new TopicExchange(exchange);
    }

    @Bean
    public Queue userRegisteredQueue() {
        return QueueBuilder.durable("notification.user-registered")
                .withArgument("x-dead-letter-exchange", "auth.events.dlq")
                .withArgument("x-dead-letter-routing-key", "dead-letter")
                .build();
    }

    @Bean
    public TopicExchange dlqExchange() {
        return new TopicExchange("auth.events.dlq");
    }

    @Bean
    public Queue dlqQueue() {
        return QueueBuilder.durable("notification.user-registered.dlq").build();
    }

    @Bean
    public Binding dlqBinding(Queue dlqQueue, TopicExchange dlqExchange) {
        return BindingBuilder.bind(dlqQueue).to(dlqExchange).with("dead-letter");
    }

    @Bean
    public Binding userRegisteredBinding(Queue userRegisteredQueue, TopicExchange authExchange) {
        return BindingBuilder.bind(userRegisteredQueue).to(authExchange).with(userRegisteredRoutingKey);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return new Jackson2JsonMessageConverter(mapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }
}
