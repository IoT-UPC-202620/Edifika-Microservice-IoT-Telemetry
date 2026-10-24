package com.edifika.telemetry.infrastructure.messaging.rabbitmq.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;

import java.util.Map;

@Configuration
public class RabbitMqConfiguration {

    public static final String EVENTS_EXCHANGE = "edifika.events";
    public static final String COMMANDS_EXCHANGE = "edifika.edge.commands";
    public static final String DLX_EXCHANGE = "edifika.dlx";

    public static final String EDGE_EVENTS_QUEUE = "telemetry.edge-events";
    public static final String EDGE_EVENTS_DLQ = "telemetry.edge-events.dlq";

    public static final String DEVICE_REGISTRY_QUEUE = "telemetry.device-registry";
    public static final String DEVICE_REGISTRY_DLQ = "telemetry.device-registry.dlq";

    @Bean
    public TopicExchange eventsExchange() {
        return new TopicExchange(EVENTS_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange commandsExchange() {
        return new TopicExchange(COMMANDS_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange dlxExchange() {
        return new TopicExchange(DLX_EXCHANGE, true, false);
    }

    @Bean
    public Queue edgeEventsQueue() {
        return QueueBuilder.durable(EDGE_EVENTS_QUEUE)
                .withArguments(Map.of(
                        "x-dead-letter-exchange", DLX_EXCHANGE,
                        "x-dead-letter-routing-key", EDGE_EVENTS_QUEUE
                ))
                .build();
    }

    @Bean
    public Queue edgeEventsDlq() {
        return QueueBuilder.durable(EDGE_EVENTS_DLQ).build();
    }

    @Bean
    public Binding edgeEventsDlqBinding(Queue edgeEventsDlq, TopicExchange dlxExchange) {
        return BindingBuilder.bind(edgeEventsDlq).to(dlxExchange).with(EDGE_EVENTS_QUEUE);
    }

    @Bean
    public Binding readingBinding(Queue edgeEventsQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(edgeEventsQueue).to(eventsExchange).with("edge.reading");
    }

    @Bean
    public Binding alertBinding(Queue edgeEventsQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(edgeEventsQueue).to(eventsExchange).with("edge.alert");
    }

    @Bean
    public Binding deviceStatusBinding(Queue edgeEventsQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(edgeEventsQueue).to(eventsExchange).with("edge.device-status");
    }

    @Bean
    public Queue deviceRegistryQueue() {
        return QueueBuilder.durable(DEVICE_REGISTRY_QUEUE)
                .withArguments(Map.of(
                        "x-dead-letter-exchange", DLX_EXCHANGE,
                        "x-dead-letter-routing-key", DEVICE_REGISTRY_QUEUE
                ))
                .build();
    }

    @Bean
    public Queue deviceRegistryDlq() {
        return QueueBuilder.durable(DEVICE_REGISTRY_DLQ).build();
    }

    @Bean
    public Binding deviceRegistryDlqBinding(Queue deviceRegistryDlq, TopicExchange dlxExchange) {
        return BindingBuilder.bind(deviceRegistryDlq).to(dlxExchange).with(DEVICE_REGISTRY_QUEUE);
    }

    @Bean
    public Binding deviceRegisteredBinding(Queue deviceRegistryQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(deviceRegistryQueue).to(eventsExchange).with("iot.device-registered");
    }

    @Bean
    public Binding deviceUpdatedBinding(Queue deviceRegistryQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(deviceRegistryQueue).to(eventsExchange).with("iot.device-updated");
    }

    @Bean
    public Binding deviceDecommissionedBinding(Queue deviceRegistryQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(deviceRegistryQueue).to(eventsExchange).with("iot.device-decommissioned");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return new Jackson2JsonMessageConverter(mapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }

    @Bean
    public org.aopalliance.intercept.MethodInterceptor retryInterceptor() {
        return RetryInterceptorBuilder.stateless()
                .maxRetries(3)
                .backOffOptions(1000L, 2.0, 4000L)
                .recoverer(new RejectAndDontRequeueRecoverer())
                .build();
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter jsonMessageConverter,
            org.aopalliance.intercept.MethodInterceptor retryInterceptor
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);
        factory.setAdviceChain(retryInterceptor);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }
}
