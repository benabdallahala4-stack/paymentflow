package com.paymentflow.payment.infrastructure;

import com.paymentflow.payment.domain.OutboxEvent;
import com.paymentflow.shared.infrastructure.KafkaTopicConfig;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Scheduled poller for the transactional outbox (ADR-006, rule 6). Reads unpublished
 * rows, publishes to Kafka, marks them published. If Kafka is unreachable the publish
 * future fails, is logged, and the row is simply retried on the next poll - the app
 * must keep serving HTTP either way, so nothing here throws out of the scheduled method.
 */
@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);
    private static final int BATCH_SIZE = 50;

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxPublisher(OutboxEventRepository outboxEventRepository, KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelayString = "${paymentflow.outbox.poll-interval-ms:2000}")
    @Transactional
    public void publishPending() {
        List<OutboxEvent> pending = outboxEventRepository.findUnpublished(PageRequest.of(0, BATCH_SIZE));
        for (OutboxEvent event : pending) {
            try {
                kafkaTemplate.send(KafkaTopicConfig.PAYMENT_EVENTS_TOPIC, event.getAggregateId().toString(),
                                event.getPayload())
                        .get(2, java.util.concurrent.TimeUnit.SECONDS);
                event.markPublished();
                outboxEventRepository.save(event);
            } catch (Exception e) {
                // Kafka unreachable or send failed: log and leave unpublished for the
                // next poll. The application must never crash or stop serving HTTP
                // because Kafka is down (rule 6) - the outbox row is the durable record.
                log.warn("failed to publish outbox event {} ({}), will retry on next poll: {}",
                        event.getId(), event.getEventType(), e.getMessage());
            }
        }
    }
}
