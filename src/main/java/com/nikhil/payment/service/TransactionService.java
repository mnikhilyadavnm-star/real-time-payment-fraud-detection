package com.nikhil.payment.service;

import com.nikhil.payment.dto.TransactionRequest;
import com.nikhil.payment.dto.TransactionResponse;
import com.nikhil.payment.kafka.TransactionProducer;
import com.nikhil.payment.model.FraudDecision;
import com.nikhil.payment.model.Transaction;
import com.nikhil.payment.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

    private final TransactionRepository repository;
    private final FraudEvaluationService fraudEvaluationService;
    private final TransactionProducer producer;

    public TransactionService(TransactionRepository repository,
                              FraudEvaluationService fraudEvaluationService,
                              TransactionProducer producer) {
        this.repository = repository;
        this.fraudEvaluationService = fraudEvaluationService;
        this.producer = producer;
    }

    /** Thrown when the client-supplied {@code externalReference} was already used. */
    public static class DuplicateTransactionException extends RuntimeException {
        public DuplicateTransactionException(String externalReference) {
            super("Duplicate transaction: externalReference '" + externalReference + "' already exists");
        }
    }

    /**
     * Submit a transaction: validate the idempotency key, persist as
     * PENDING, publish the Kafka event, evaluate fraud synchronously,
     * cache the decision in Redis, and return the decided transaction.
     */
    @Transactional
    public TransactionResponse submit(TransactionRequest request) {
        if (request.externalReference() != null && !request.externalReference().isBlank()
                && repository.existsByExternalReference(request.externalReference())) {
            throw new DuplicateTransactionException(request.externalReference());
        }

        Transaction transaction = new Transaction(
                request.externalReference(),
                request.accountId(),
                request.amount(),
                request.currency(),
                request.merchantId());
        transaction = repository.save(transaction); // PENDING
        log.info("Saved transaction {} as PENDING", transaction.getId());

        producer.publish(transaction);

        FraudDecision decision = fraudEvaluationService.evaluateTransaction(transaction);
        transaction.applyDecision(decision);
        transaction = repository.save(transaction);
        fraudEvaluationService.cacheDecision(transaction);
        log.info("Transaction {} decided {}: {}", transaction.getId(),
                decision.outcome(), decision.reasons());

        return TransactionResponse.from(transaction);
    }

    @Transactional(readOnly = true)
    public TransactionResponse getById(Long id) {
        Transaction transaction = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Transaction not found: " + id));
        return TransactionResponse.from(transaction);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> listRecent(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        return repository.findAll(PageRequest.of(0, safeLimit,
                        org.springframework.data.domain.Sort.by(
                                org.springframework.data.domain.Sort.Direction.DESC, "createdAt")))
                .stream()
                .map(TransactionResponse::from)
                .toList();
    }
}
