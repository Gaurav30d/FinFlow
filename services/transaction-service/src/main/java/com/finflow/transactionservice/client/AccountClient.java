package com.finflow.transactionservice.client;

import com.finflow.transactionservice.dto.AccountResponse;
import com.finflow.transactionservice.exception.InsufficientBalanceException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.HttpClientErrorException;

import java.math.BigDecimal;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class AccountClient {

    private final RestClient restClient;

    public void deposit(String token, BigDecimal amount) {

        restClient.post()
                .uri("/accounts/me/deposit")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("amount", amount))
                .retrieve()
                .toBodilessEntity();
    }

    public void withdraw(String token, BigDecimal amount) {

        try {

            restClient.post()
                    .uri("/accounts/me/withdraw")
                    .header("Authorization", token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("amount", amount))
                    .retrieve()
                    .toBodilessEntity();

        } catch (HttpClientErrorException.BadRequest exception) {

            throw new InsufficientBalanceException(
                    "Insufficient balance"
            );
        }
    }
    public Long getReceiverUserId(
            String token,
            String accountNumber) {

        AccountResponse response = restClient.get()
                .uri("/accounts/by-number/{accountNumber}", accountNumber)
                .header("Authorization", token)
                .retrieve()
                .body(AccountResponse.class);

        return response.getUserId();
    }

    public void depositToAccount(
            String token,
            String accountNumber,
            BigDecimal amount) {

        restClient.post()
                .uri("/accounts/internal/{accountNumber}/deposit", accountNumber)
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("amount", amount))
                .retrieve()
                .toBodilessEntity();
    }

    public void refund(
            String token,
            Long userId,
            BigDecimal amount) {

        restClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/accounts/internal/refund")
                        .queryParam("userId", userId)
                        .build())
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("amount", amount))
                .retrieve()
                .toBodilessEntity();
    }
}