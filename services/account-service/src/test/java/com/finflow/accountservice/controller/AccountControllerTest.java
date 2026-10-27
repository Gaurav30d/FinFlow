package com.finflow.accountservice.controller;

import com.finflow.accountservice.dto.DepositRequest;
import com.finflow.accountservice.dto.WithdrawRequest;
import com.finflow.accountservice.entity.Account;
import com.finflow.accountservice.entity.AccountStatus;
import com.finflow.accountservice.exception.GlobalExceptionHandler;
import com.finflow.accountservice.exception.InsufficientBalanceException;
import com.finflow.accountservice.service.AccountService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AccountControllerTest {

    @Mock
    private AccountService accountService;

    @InjectMocks
    private AccountController accountController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private Account sampleAccount;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(accountController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();

        sampleAccount = Account.builder()
                .id(1L)
                .accountNumber("1234567812345678")
                .userId(100L)
                .balance(new BigDecimal("1000.00"))
                .currency("INR")
                .status(AccountStatus.ACTIVE)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("GET /accounts/{accountId}: returns 200 OK when authenticated user owns account")
    void getAccount_ownAccount_returns200Ok() throws Exception {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                100L, null, Collections.emptyList()
        );

        when(accountService.getAccount(1L, 100L)).thenReturn(sampleAccount);

        mockMvc.perform(get("/accounts/1")
                        .principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.userId").value(100L))
                .andExpect(jsonPath("$.accountNumber").value("1234567812345678"))
                .andExpect(jsonPath("$.balance").value(1000.00));

        verify(accountService).getAccount(1L, 100L);
    }

    @Test
    @DisplayName("GET /accounts/{accountId}: returns 403 Forbidden when user attempts cross-user access")
    void getAccount_crossUserAccess_returns403Forbidden() throws Exception {
        // Authenticated as user 200, but requesting account 1 (owned by user 100)
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                200L, null, Collections.emptyList()
        );

        when(accountService.getAccount(1L, 200L))
                .thenThrow(new AccessDeniedException("Access denied"));

        mockMvc.perform(get("/accounts/1")
                        .principal(auth))
                .andExpect(status().isForbidden())
                .andExpect(content().string("Access denied"));

        verify(accountService).getAccount(1L, 200L);
    }

    @Test
    @DisplayName("GET /accounts/{accountId}: returns 200 OK when called by internal service (ROLE_SERVICE)")
    void getAccount_internalService_returns200Ok() throws Exception {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "transaction-service", null, AuthorityUtils.createAuthorityList("ROLE_SERVICE")
        );

        when(accountService.getAccount(1L)).thenReturn(sampleAccount);

        mockMvc.perform(get("/accounts/1")
                        .principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.userId").value(100L));

        verify(accountService).getAccount(1L);
        verify(accountService, never()).getAccount(eq(1L), any());
    }

    @Test
    @DisplayName("GET /accounts/{accountId}: returns 404 Not Found when account does not exist")
    void getAccount_notFound_returns404NotFound() throws Exception {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                100L, null, Collections.emptyList()
        );

        when(accountService.getAccount(999L, 100L))
                .thenThrow(new RuntimeException("Account not found"));

        mockMvc.perform(get("/accounts/999")
                        .principal(auth))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Account not found"));
    }

    @Test
    @DisplayName("POST /accounts/me/deposit: returns 200 OK with updated balance")
    void deposit_success_returns200Ok() throws Exception {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                100L, null, Collections.emptyList()
        );

        DepositRequest request = new DepositRequest();
        request.setAmount(new BigDecimal("250.00"));

        Account depositedAccount = Account.builder()
                .id(1L)
                .accountNumber("1234567812345678")
                .userId(100L)
                .balance(new BigDecimal("1250.00"))
                .currency("INR")
                .status(AccountStatus.ACTIVE)
                .createdAt(Instant.now())
                .build();

        when(accountService.deposit(100L, new BigDecimal("250.00"))).thenReturn(depositedAccount);

        mockMvc.perform(post("/accounts/me/deposit")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(1250.00));
    }

    @Test
    @DisplayName("POST /accounts/me/withdraw: returns 400 Bad Request on insufficient balance")
    void withdraw_insufficientBalance_returns400BadRequest() throws Exception {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                100L, null, Collections.emptyList()
        );

        WithdrawRequest request = new WithdrawRequest();
        request.setAmount(new BigDecimal("5000.00"));

        when(accountService.withdraw(100L, new BigDecimal("5000.00")))
                .thenThrow(new InsufficientBalanceException("Insufficient balance"));

        mockMvc.perform(post("/accounts/me/withdraw")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Insufficient balance"));
    }

    @Test
    @DisplayName("POST /accounts/me/deposit: returns 400 Bad Request when amount is below minimum (0.01)")
    void deposit_invalidAmount_returns400BadRequest() throws Exception {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                100L, null, Collections.emptyList()
        );

        DepositRequest request = new DepositRequest();
        request.setAmount(new BigDecimal("0.00"));

        mockMvc.perform(post("/accounts/me/deposit")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(accountService, never()).deposit(any(), any());
    }

    @Test
    @DisplayName("POST /accounts/internal/{accountNumber}/deposit: returns 200 OK for internal recipient deposit")
    void internalDepositToAccount_returns200Ok() throws Exception {
        DepositRequest request = new DepositRequest();
        request.setAmount(new BigDecimal("500.00"));

        Account credited = Account.builder()
                .id(2L)
                .accountNumber("9876543210987654")
                .userId(200L)
                .balance(new BigDecimal("800.00"))
                .currency("INR")
                .status(AccountStatus.ACTIVE)
                .createdAt(Instant.now())
                .build();

        when(accountService.depositToAccount("9876543210987654", new BigDecimal("500.00")))
                .thenReturn(credited);

        mockMvc.perform(post("/accounts/internal/9876543210987654/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("9876543210987654"))
                .andExpect(jsonPath("$.balance").value(800.00));
    }
}
