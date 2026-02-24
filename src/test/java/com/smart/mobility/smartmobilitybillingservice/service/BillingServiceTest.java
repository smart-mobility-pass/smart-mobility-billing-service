package com.smart.mobility.smartmobilitybillingservice.service;

import com.smart.mobility.smartmobilitybillingservice.dto.CreateAccountRequest;
import com.smart.mobility.smartmobilitybillingservice.dto.TripPricedEvent;
import com.smart.mobility.smartmobilitybillingservice.enums.TransactionStatus;
import com.smart.mobility.smartmobilitybillingservice.exception.AccountNotFoundException;
import com.smart.mobility.smartmobilitybillingservice.messaging.PaymentEventPublisher;
import com.smart.mobility.smartmobilitybillingservice.model.Account;
import com.smart.mobility.smartmobilitybillingservice.model.Transaction;
import com.smart.mobility.smartmobilitybillingservice.repository.AccountRepository;
import com.smart.mobility.smartmobilitybillingservice.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BillingServiceTest {

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private PaymentEventPublisher eventPublisher;

    @InjectMocks
    private BillingService billingService;

    @BeforeEach
    void setUp() {
        // Inject the @Value field manually since we're running unit tests (no Spring
        // context)
        ReflectionTestUtils.setField(billingService, "dailyCap", new BigDecimal("50000"));
    }

    // ─────────────────────────────────────────────────────────────
    // Account Creation
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("createAccount: creates account with XOF currency and zero balance")
    void createAccount_success() {
        Long userId = 1L;
        when(accountRepository.existsByUserId(userId)).thenReturn(false);
        when(accountRepository.save(any())).thenAnswer(inv -> {
            Account a = inv.getArgument(0);
            ReflectionTestUtils.setField(a, "id", 10L);
            return a;
        });

        var response = billingService.createAccount(new CreateAccountRequest(userId, null));

        assertThat(response.balance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.currency()).isEqualTo("XOF");
        verify(accountRepository).save(any(Account.class));
    }

    // ─────────────────────────────────────────────────────────────
    // Top-Up
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("topUp: correctly adds amount to balance")
    void topUp_success() {
        Long userId = 1L;
        Account account = buildAccount(userId, "500.00", "0.00");
        when(accountRepository.findByUserId(userId)).thenReturn(Optional.of(account));
        when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = billingService.topUp(userId, new BigDecimal("1000.00"), "Recharge");

        assertThat(response.balance()).isEqualByComparingTo(new BigDecimal("1500.00"));
        verify(transactionRepository).save(any(Transaction.class));
    }

    // ─────────────────────────────────────────────────────────────
    // Debit — Success
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("processDebit: debits account and publishes PAYMENT_COMPLETED")
    void processDebit_success() {
        Long userId = 2L;
        String tripId = "TRIP-001";
        Account account = buildAccount(userId, "10000.00", "0.00");

        when(transactionRepository.findByTripId(tripId)).thenReturn(Optional.empty());
        when(accountRepository.findByUserId(userId)).thenReturn(Optional.of(account));
        when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        billingService.processDebit(new TripPricedEvent(tripId, userId, new BigDecimal("500.00")));

        assertThat(account.getBalance()).isEqualByComparingTo(new BigDecimal("9500.00"));
        assertThat(account.getDailySpent()).isEqualByComparingTo(new BigDecimal("500.00"));
        verify(transactionRepository).save(argThat(tx -> tx.getStatus() == TransactionStatus.SUCCESS));
        verify(eventPublisher).publishPaymentCompleted(eq(tripId), eq(userId), any());
    }

    // ─────────────────────────────────────────────────────────────
    // Debit — Insufficient Balance
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("processDebit: records FAILED transaction when balance is insufficient")
    void processDebit_insufficientBalance() {
        Long userId = 3L;
        String tripId = "TRIP-002";
        Account account = buildAccount(userId, "100.00", "0.00"); // only 100 XOF

        when(transactionRepository.findByTripId(tripId)).thenReturn(Optional.empty());
        when(accountRepository.findByUserId(userId)).thenReturn(Optional.of(account));

        billingService.processDebit(new TripPricedEvent(tripId, userId, new BigDecimal("500.00")));

        verify(transactionRepository).save(argThat(tx -> tx.getStatus() == TransactionStatus.FAILED));
        verify(eventPublisher).publishPaymentFailed(eq(tripId), eq(userId), any(), anyString());
        // balance must be unchanged
        assertThat(account.getBalance()).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    // ─────────────────────────────────────────────────────────────
    // Debit — Daily Cap
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("processDebit: amount is trimmed to daily cap remainder")
    void processDebit_dailyCapTrims() {
        Long userId = 4L;
        String tripId = "TRIP-003";
        // Already spent 49000, cap is 50000 → only 1000 remaining
        Account account = buildAccount(userId, "30000.00", "49000.00");

        when(transactionRepository.findByTripId(tripId)).thenReturn(Optional.empty());
        when(accountRepository.findByUserId(userId)).thenReturn(Optional.of(account));
        when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        billingService.processDebit(new TripPricedEvent(tripId, userId, new BigDecimal("5000.00")));

        // Only 1000 should be debited (cap remainder)
        assertThat(account.getBalance()).isEqualByComparingTo(new BigDecimal("29000.00"));
        assertThat(account.getDailySpent()).isEqualByComparingTo(new BigDecimal("50000.00"));
        verify(eventPublisher).publishPaymentCompleted(eq(tripId), eq(userId),
                argThat(a -> a.compareTo(new BigDecimal("1000.00")) == 0));
    }

    @Test
    @DisplayName("processDebit: FAILED when daily cap is fully exhausted")
    void processDebit_dailyCapExhausted() {
        Long userId = 5L;
        String tripId = "TRIP-004";
        Account account = buildAccount(userId, "30000.00", "50000.00"); // cap fully used

        when(transactionRepository.findByTripId(tripId)).thenReturn(Optional.empty());
        when(accountRepository.findByUserId(userId)).thenReturn(Optional.of(account));

        billingService.processDebit(new TripPricedEvent(tripId, userId, new BigDecimal("100.00")));

        verify(transactionRepository).save(argThat(tx -> tx.getStatus() == TransactionStatus.FAILED));
        verify(eventPublisher).publishPaymentFailed(eq(tripId), eq(userId), any(), anyString());
    }

    // ─────────────────────────────────────────────────────────────
    // Idempotence
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("processDebit: skips duplicate TRIP_PRICED events (idempotence)")
    void processDebit_idempotence() {
        String tripId = "TRIP-005";
        Long userId = 6L;
        // Simulate that this tripId was already processed
        when(transactionRepository.findByTripId(tripId)).thenReturn(Optional.of(mock(Transaction.class)));

        billingService.processDebit(new TripPricedEvent(tripId, userId, new BigDecimal("200.00")));

        // Nothing else should have been called
        verifyNoInteractions(accountRepository);
        verifyNoInteractions(eventPublisher);
    }

    // ─────────────────────────────────────────────────────────────
    // Account Not Found
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAccountByUserId: throws AccountNotFoundException for unknown user")
    void getAccount_notFound() {
        when(accountRepository.findByUserId(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> billingService.getAccountByUserId(99L))
                .isInstanceOf(AccountNotFoundException.class);
    }

    // ─────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────

    private Account buildAccount(Long userId, String balance, String dailySpent) {
        return Account.builder()
                .id(userId * 10)
                .userId(userId)
                .balance(new BigDecimal(balance))
                .dailySpent(new BigDecimal(dailySpent))
                .currency("XOF")
                .version(0L)
                .build();
    }
}
