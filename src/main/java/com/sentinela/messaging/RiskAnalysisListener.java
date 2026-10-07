package com.sentinela.messaging;

import com.sentinela.service.RiskMessageProcessor;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("worker")
public class RiskAnalysisListener {

    private final RiskMessageProcessor processor;
    private final MeterRegistry meterRegistry;

    public RiskAnalysisListener(RiskMessageProcessor processor, MeterRegistry meterRegistry) {
        this.processor = processor;
        this.meterRegistry = meterRegistry;
    }

    @RabbitListener(queues = RabbitTopology.QUEUE, containerFactory = "rabbitListenerContainerFactory")
    public void analyze(String transactionId) {
        Timer.Sample sample = Timer.start(meterRegistry);
        String result = "error";
        try {
            boolean processed = processor.process(transactionId);
            result = processed ? "processed" : "duplicate";
            meterRegistry.counter("sentinela.risk.messages", "result", result).increment();
        } catch (RuntimeException exception) {
            meterRegistry.counter("sentinela.risk.messages", "result", result).increment();
            throw exception;
        } finally {
            sample.stop(meterRegistry.timer("sentinela.risk.processing", "result", result));
        }
    }
}
