package com.paymentflow.payment.infrastructure;

import com.paymentflow.shared.infrastructure.KafkaTopicConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Basic no-op "audit" consumer (rule 6) demonstrating the async side of the outbox ->
 * Kafka -> consumer pipeline. In a real system this would fan out to
 * notifications/analytics; here it just logs, proving delivery without adding scope.
 */
@Component
public class PaymentAuditConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentAuditConsumer.class);

    @KafkaListener(topics = KafkaTopicConfig.PAYMENT_EVENTS_TOPIC, groupId = "audit-consumer")
    public void onPaymentEvent(String payload) {
        log.info("[audit] received payment event: {}", payload);
    }
}
