package com.nikhil.payment.repository;

import com.nikhil.payment.model.Transaction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByExternalReference(String externalReference);

    boolean existsByExternalReference(String externalReference);

    List<Transaction> findByAccountIdOrderByCreatedAtDesc(String accountId, Pageable pageable);

    long countByAccountIdAndCreatedAtAfter(String accountId, Instant after);
}
