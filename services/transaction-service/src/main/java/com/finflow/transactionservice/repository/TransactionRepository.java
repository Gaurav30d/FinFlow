package com.finflow.transactionservice.repository;

import com.finflow.transactionservice.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByTransactionReference(UUID transactionReference);

    List<Transaction> findByUserIdOrderByCreatedAtDesc(Long userId);
}