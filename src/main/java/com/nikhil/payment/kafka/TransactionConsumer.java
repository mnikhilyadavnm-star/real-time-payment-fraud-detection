package com.nikhil.payment.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nikhil.payment.model.FraudDecision;
import com.nikhil.payment.model.Transaction;
import com.nikhil.payment.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes {@code transactions} events.
 *
 * <p>The submit path already evaluates fraud synchronously and returns
 * the decision, so this consumer is intentionally simple and
 * idempotent: it logs the event, and only acts if the transaction is
 * still PENDING (for example, if it was created before an evaluation
 * completed). Already-decided transactions are skipped, so re-delivery
 * of the same event is harmless.</p>
 */
@Component
public class TransactionConsumer {

    private static final Logger log = LoggerFactory.getLogger(TransactionConsumer.class);

    private final TransactionRepository repository;
    private final ObjectMapper objectMapper;

    public TransactionConsumer(TransactionRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = TransactionProducer.TOPIC, groupId = "fraud-detection")
    public void onTransaction(String payload) {
        log.info("Received transaction event: {}", payload);
        try {
            JsonNode event = objectMapper.readTree(payload);
            if (!event.hasNonNull("transactionId")) {
                log.warn("Transaction event has no transactionId, skipping");
                return;
            }
            long id = event.get("transactionId").asLong();
            Transaction transaction = repository.findById(id).orElse(null);
            if (transaction == null) {
                log.warn("Transaction {} from Kafka not found in PostgreSQL", id);
                return;
            }
            if (transaction.getStatus() != FraudDecision.Outcome.PENDING) {
                // Idempotent: already decided, nothing to do.
                log.info("Transaction {} already decided {} — skipping", id, transaction.getStatus());
                return;
            }
            log.info("Transaction {} is still PENDING after Kafka delivery; "
                    + "it will be decided on the next lookup / re-evaluation", id);
        } catch (Exception e) {
            log.error("Could not process transaction event: {}", e.getMessage());
        }
    }
}
