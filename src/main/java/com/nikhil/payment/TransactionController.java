package com.nikhil.payment;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Stub REST controller for the Real-Time Payment and Fraud Detection Platform.
 *
 * <p>Endpoints are placeholders in this starter skeleton and return
 * 501 Not Implemented until transaction submission, Kafka publishing,
 * fraud evaluation, and persistence are implemented.</p>
 */
@RestController
@RequestMapping("/transactions")
public class TransactionController {

    @PostMapping
    public ResponseEntity<Map<String, String>> submitTransaction(
            @RequestBody Map<String, Object> transaction) {
        // TODO: publish transaction event to Kafka, evaluate for fraud,
        // persist to PostgreSQL, and return the decision.
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(Map.of("message", "TODO: submit transaction for fraud evaluation"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, String>> getTransaction(@PathVariable String id) {
        // TODO: look up the transaction and its fraud decision in PostgreSQL.
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(Map.of("message", "TODO: get transaction " + id));
    }
}
