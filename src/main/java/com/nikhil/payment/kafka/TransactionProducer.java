package com.nikhil.payment.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nikhil.payment.model.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/** Publishes transaction events to the {@code transactions} Kafka topic. */
@Component
public class TransactionProducer {

    public static final String TOPIC = "transactions";

    private static final Logger log = LoggerFactory.getLogger(TransactionProducer.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public TransactionProducer(KafkaTemplate<String, String> kafkaTemplate,
                               ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void publish(Transaction transaction) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("transactionId", transaction.getId());
        event.put("externalReference", transaction.getExternalReference());
        event.put("accountId", transaction.getAccountId());
        event.put("amount", transaction.getAmount());
        event.put("currency", transaction.getCurrency());
        event.put("merchantId", transaction.getMerchantId());
        event.put("status", transaction.getStatus().name());
        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(TOPIC, String.valueOf(transaction.getId()), payload);
            log.info("Published transaction {} to topic {}", transaction.getId(), TOPIC);
        } catch (JsonProcessingException e) {
            log.error("Could not serialise transaction {}: {}", transaction.getId(), e.getMessage());
            throw new IllegalStateException("Could not publish transaction event", e);
        }
    }
}
