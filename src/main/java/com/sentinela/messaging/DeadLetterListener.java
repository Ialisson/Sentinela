package com.sentinela.messaging;

import com.sentinela.service.DeadLetterFailureHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("api")
public class DeadLetterListener {
    private final DeadLetterFailureHandler failureHandler;

    public DeadLetterListener(DeadLetterFailureHandler failureHandler) {
        this.failureHandler = failureHandler;
    }

    @RabbitListener(queues = RabbitTopology.DEAD_LETTER_QUEUE, containerFactory = "rabbitListenerContainerFactory")
    public void markFailed(String transactionId) {
        failureHandler.markFailed(transactionId);
    }
}
