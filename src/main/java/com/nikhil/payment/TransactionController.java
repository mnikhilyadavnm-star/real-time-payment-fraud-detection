package com.nikhil.payment;

import com.nikhil.payment.dto.TransactionRequest;
import com.nikhil.payment.dto.TransactionResponse;
import com.nikhil.payment.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * REST API for the Real-Time Payment and Fraud Detection Platform.
 *
 * <p>All endpoints are implemented — no stubs. Fraud decisions use the
 * illustrative rules in {@link com.nikhil.payment.service.FraudEvaluationService},
 * not production fraud rules.</p>
 */
@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    /** Submit a transaction; returns 201 with the fraud decision. */
    @PostMapping
    public ResponseEntity<TransactionResponse> submitTransaction(
            @Valid @RequestBody TransactionRequest request) {
        TransactionResponse response = transactionService.submit(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** Get one transaction (with its decision) by database id. */
    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> getTransaction(@PathVariable Long id) {
        return ResponseEntity.ok(transactionService.getById(id));
    }

    /** List the most recent transactions, newest first. Default 20, max 100. */
    @GetMapping
    public ResponseEntity<List<TransactionResponse>> listTransactions(
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(transactionService.listRecent(limit));
    }

    @ExceptionHandler(TransactionService.DuplicateTransactionException.class)
    public ResponseEntity<Map<String, String>> handleDuplicate(
            TransactionService.DuplicateTransactionException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(NoSuchElementException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
    }
}
