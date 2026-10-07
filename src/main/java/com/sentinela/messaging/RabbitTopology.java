package com.sentinela.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.ConditionalRejectingErrorHandler;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;

@Configuration
public class RabbitTopology {

    public static final String EXCHANGE = "sentinela.risk";
    public static final String ROUTING_KEY = "risk.analyze";
    public static final String QUEUE = "sentinela.risk.analysis";
    public static final String DEAD_LETTER_EXCHANGE = "sentinela.risk.dlx";
    public static final String DEAD_LETTER_QUEUE = "sentinela.risk.analysis.dlq";

    @Bean
    DirectExchange riskExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    @Bean
    DirectExchange deadLetterExchange() {
        return new DirectExchange(DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean
    Queue riskQueue() {
        return QueueBuilder.durable(QUEUE)
                .deadLetterExchange(DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(DEAD_LETTER_QUEUE)
                .build();
    }

    @Bean
    Queue deadLetterQueue() {
        return QueueBuilder.durable(DEAD_LETTER_QUEUE).build();
    }

    @Bean
    Binding riskBinding(@Qualifier("riskQueue") Queue riskQueue,
                        @Qualifier("riskExchange") DirectExchange riskExchange) {
        return BindingBuilder.bind(riskQueue).to(riskExchange).with(ROUTING_KEY);
    }

    @Bean
    Binding deadLetterBinding(@Qualifier("deadLetterQueue") Queue deadLetterQueue,
                              @Qualifier("deadLetterExchange") DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with(DEAD_LETTER_QUEUE);
    }

    @Bean
    MethodInterceptor riskRetryInterceptor() {
        return RetryInterceptorBuilder.stateless()
                .maxRetries(3)
                .backOffOptions(500, 2.0, 5_000)
                .recoverer(new RejectAndDontRequeueRecoverer())
                .build();
    }

    @Bean
    SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory, MethodInterceptor riskRetryInterceptor,
            @Value("${spring.rabbitmq.listener.simple.auto-startup:true}") boolean autoStartup) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setAutoStartup(autoStartup);
        factory.setMessageConverter(new SimpleMessageConverter());
        factory.setAdviceChain(riskRetryInterceptor);
        factory.setDefaultRequeueRejected(false);
        factory.setConcurrentConsumers(2);
        factory.setMaxConcurrentConsumers(8);
        factory.setPrefetchCount(10);
        factory.setErrorHandler(new ConditionalRejectingErrorHandler());
        factory.setObservationEnabled(true);
        return factory;
    }
}
