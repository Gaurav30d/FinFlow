package com.finflow.accountservice.service;

import com.finflow.accountservice.entity.Account;
import com.finflow.accountservice.entity.AccountStatus;
import com.finflow.accountservice.event.AccountAuditEvent;
import com.finflow.accountservice.event.AccountEventProducer;
import com.finflow.accountservice.exception.InsufficientBalanceException;
import com.finflow.accountservice.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountEventProducer accountEventProducer;

    @Captor
    private ArgumentCaptor<AccountAuditEvent> eventCaptor;

    @InjectMocks
    private AccountService accountService;

    private Account testAccount;

    @BeforeEach
    void setUp() {
        testAccount = Account.builder()
                .id(1L)
                .accountNumber("1234567812345678")
                .userId(100L)
                .balance(new BigDecimal("500.00"))
                .currency("INR")
                .status(AccountStatus.ACTIVE)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("createAccount: creates active account and publishes ACCOUNT_CREATED event")
    void createAccount_success() {
        when(accountRepository.existsByUserId(100L)).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Account created = accountService.createAccount(100L);

        assertNotNull(created);
        assertEquals(100L, created.getUserId());
        assertEquals(BigDecimal.ZERO, created.getBalance());
        assertEquals("INR", created.getCurrency());
        assertEquals(AccountStatus.ACTIVE, created.getStatus());
        assertNotNull(created.getAccountNumber());
        assertEquals(16, created.getAccountNumber().length());

        verify(accountRepository).existsByUserId(100L);
        verify(accountRepository).save(any(Account.class));

        // Verify audit event publishing
        verify(accountEventProducer).publishEvent(eventCaptor.capture());
        AccountAuditEvent publishedEvent = eventCaptor.getValue();
        assertNotNull(publishedEvent.getEventId());
        assertEquals("ACCOUNT_CREATED", publishedEvent.getEventType());
        assertEquals(100L, publishedEvent.getActorUserId());
        assertEquals("ACCOUNT", publishedEvent.getResourceType());
        assertEquals(created.getAccountNumber(), publishedEvent.getResourceId());
        assertEquals("SUCCESS", publishedEvent.getOutcome());
        assertEquals("account-service", publishedEvent.getServiceName());
        assertNotNull(publishedEvent.getOccurredAt());
    }

    @Test
    @DisplayName("createAccount: throws RuntimeException and does not publish event when account already exists")
    void createAccount_alreadyExists_throwsException() {
        when(accountRepository.existsByUserId(100L)).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> accountService.createAccount(100L));

        assertTrue(exception.getMessage().contains("already exists"));
        verify(accountRepository, never()).save(any(Account.class));
        verify(accountEventProducer, never()).publishEvent(any());
    }

    @Test
    @DisplayName("deposit: increases balance using pessimistic lock and publishes ACCOUNT_DEPOSIT event")
    void deposit_success() {
        when(accountRepository.findByUserIdForUpdate(100L)).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Account updated = accountService.deposit(100L, new BigDecimal("150.00"));

        assertEquals(new BigDecimal("650.00"), updated.getBalance());
        verify(accountRepository).findByUserIdForUpdate(100L);
        verify(accountRepository).save(testAccount);

        // Verify audit event
        verify(accountEventProducer).publishEvent(eventCaptor.capture());
        AccountAuditEvent publishedEvent = eventCaptor.getValue();
        assertNotNull(publishedEvent.getEventId());
        assertEquals("ACCOUNT_DEPOSIT", publishedEvent.getEventType());
        assertEquals(100L, publishedEvent.getActorUserId());
        assertEquals("ACCOUNT", publishedEvent.getResourceType());
        assertEquals("1234567812345678", publishedEvent.getResourceId());
        assertEquals("SUCCESS", publishedEvent.getOutcome());
        assertEquals("account-service", publishedEvent.getServiceName());
    }

    @Test
    @DisplayName("deposit: throws exception and does not publish event when account is not active")
    void deposit_notActive_throwsException() {
        testAccount.setStatus(AccountStatus.BLOCKED);
        when(accountRepository.findByUserIdForUpdate(100L)).thenReturn(Optional.of(testAccount));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> accountService.deposit(100L, new BigDecimal("100.00")));

        assertTrue(exception.getMessage().contains("not active"));
        verify(accountRepository, never()).save(any(Account.class));
        verify(accountEventProducer, never()).publishEvent(any());
    }

    @Test
    @DisplayName("withdraw: decreases balance and publishes ACCOUNT_WITHDRAW event when balance is sufficient")
    void withdraw_success() {
        when(accountRepository.findByUserIdForUpdate(100L)).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Account updated = accountService.withdraw(100L, new BigDecimal("200.00"));

        assertEquals(new BigDecimal("300.00"), updated.getBalance());
        verify(accountRepository).findByUserIdForUpdate(100L);
        verify(accountRepository).save(testAccount);

        // Verify audit event
        verify(accountEventProducer).publishEvent(eventCaptor.capture());
        AccountAuditEvent publishedEvent = eventCaptor.getValue();
        assertNotNull(publishedEvent.getEventId());
        assertEquals("ACCOUNT_WITHDRAW", publishedEvent.getEventType());
        assertEquals(100L, publishedEvent.getActorUserId());
        assertEquals("ACCOUNT", publishedEvent.getResourceType());
        assertEquals("1234567812345678", publishedEvent.getResourceId());
        assertEquals("SUCCESS", publishedEvent.getOutcome());
        assertEquals("account-service", publishedEvent.getServiceName());
    }

    @Test
    @DisplayName("withdraw: throws InsufficientBalanceException and does not publish event when amount exceeds balance")
    void withdraw_insufficientBalance_throwsException() {
        when(accountRepository.findByUserIdForUpdate(100L)).thenReturn(Optional.of(testAccount));

        InsufficientBalanceException exception = assertThrows(InsufficientBalanceException.class,
                () -> accountService.withdraw(100L, new BigDecimal("600.00")));

        assertEquals("Insufficient balance", exception.getMessage());
        verify(accountRepository, never()).save(any(Account.class));
        verify(accountEventProducer, never()).publishEvent(any());
    }

    @Test
    @DisplayName("withdraw: throws exception and does not publish event when account is not active")
    void withdraw_notActive_throwsException() {
        testAccount.setStatus(AccountStatus.CLOSED);
        when(accountRepository.findByUserIdForUpdate(100L)).thenReturn(Optional.of(testAccount));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> accountService.withdraw(100L, new BigDecimal("50.00")));

        assertTrue(exception.getMessage().contains("not active"));
        verify(accountRepository, never()).save(any(Account.class));
        verify(accountEventProducer, never()).publishEvent(any());
    }

    @Test
    @DisplayName("getAccount with ownership: succeeds when authenticated user owns account")
    void getAccount_withOwnership_success() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));

        Account result = accountService.getAccount(1L, 100L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(100L, result.getUserId());
    }

    @Test
    @DisplayName("getAccount with ownership: throws AccessDeniedException when cross-user access is attempted")
    void getAccount_withOwnership_crossUserAccess_throwsAccessDeniedException() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));

        AccessDeniedException exception = assertThrows(AccessDeniedException.class,
                () -> accountService.getAccount(1L, 999L));

        assertEquals("Access denied", exception.getMessage());
    }

    @Test
    @DisplayName("getAccount internal: returns account by ID without user ownership check")
    void getAccount_internal_success() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));

        Account result = accountService.getAccount(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    @DisplayName("depositToAccount: uses findByAccountNumberForUpdate locking method and does not publish account audit event")
    void depositToAccount_usesPessimisticLockingRepositoryMethod() {
        when(accountRepository.findByAccountNumberForUpdate("1234567812345678"))
                .thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Account result = accountService.depositToAccount("1234567812345678", new BigDecimal("250.00"));

        assertEquals(new BigDecimal("750.00"), result.getBalance());
        verify(accountRepository).findByAccountNumberForUpdate("1234567812345678");
        verify(accountRepository, never()).findByAccountNumber("1234567812345678");
        verify(accountRepository).save(testAccount);

        // Internal transfer leg does not publish account-level audit event
        verify(accountEventProducer, never()).publishEvent(any());
    }

    @Test
    @DisplayName("depositToAccount: throws exception when recipient account is not active")
    void depositToAccount_notActive_throwsException() {
        testAccount.setStatus(AccountStatus.BLOCKED);
        when(accountRepository.findByAccountNumberForUpdate("1234567812345678"))
                .thenReturn(Optional.of(testAccount));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> accountService.depositToAccount("1234567812345678", new BigDecimal("100.00")));

        assertTrue(exception.getMessage().contains("not active"));
        verify(accountRepository, never()).save(any(Account.class));
        verify(accountEventProducer, never()).publishEvent(any());
    }

    @Test
    @DisplayName("refund: increases balance using pessimistic lock and does not publish account audit event")
    void refund_success() {
        when(accountRepository.findByUserIdForUpdate(100L)).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Account result = accountService.refund(100L, new BigDecimal("75.00"));

        assertEquals(new BigDecimal("575.00"), result.getBalance());
        verify(accountRepository).findByUserIdForUpdate(100L);
        verify(accountRepository).save(testAccount);

        // Internal compensation leg does not publish account-level audit event
        verify(accountEventProducer, never()).publishEvent(any());
    }
}
