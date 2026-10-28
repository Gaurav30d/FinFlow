package com.finflow.transactionservice.repository;

import com.finflow.transactionservice.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByTransactionReference(UUID transactionReference);

    @Query("""
        SELECT t
        FROM Transaction t
        WHERE t.senderUserId = :userId
           OR t.receiverUserId = :userId
        ORDER BY t.createdAt DESC
        """)
    List<Transaction> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    @Query("""
        SELECT t
        FROM Transaction t
        WHERE (t.senderUserId = :userId OR t.receiverUserId = :userId)
          AND t.status = :status
        ORDER BY t.createdAt DESC
        """)
    List<Transaction> findByUserIdAndStatusOrderByCreatedAtDesc(
            @Param("userId") Long userId,
            @Param("status") com.finflow.transactionservice.entity.TransactionStatus status
    );
}