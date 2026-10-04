package com.finflow.transactionservice.mapper;

import com.finflow.transactionservice.dto.TransactionResponse;
import com.finflow.transactionservice.entity.Transaction;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {

    public TransactionResponse toResponse(
            Transaction transaction,
            Long loggedInUserId) {

        boolean outgoing =
                transaction.getSenderUserId().equals(loggedInUserId);

        String direction = outgoing
                ? "OUTGOING"
                : "INCOMING";

        String counterpartyAccount = outgoing
                ? transaction.getReceiverAccountNumber()
                : transaction.getSenderAccountNumber();

        return TransactionResponse.builder()
                .transactionReference(
                        transaction.getTransactionReference()
                )
                .type(transaction.getType())
                .direction(direction)
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .counterpartyAccount(
                        maskAccountNumber(counterpartyAccount)
                )
                .status(transaction.getStatus())
                .createdAt(transaction.getCreatedAt())
                .build();
    }

    private String maskAccountNumber(String accountNumber) {

        if (accountNumber == null || accountNumber.length() <= 4) {
            return accountNumber;
        }

        return "••••" +
                accountNumber.substring(
                        accountNumber.length() - 4
                );
    }
}