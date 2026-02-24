package com.smart.mobility.smartmobilitybillingservice.service;

import com.smart.mobility.smartmobilitybillingservice.dto.AccountResponse;
import com.smart.mobility.smartmobilitybillingservice.dto.CreateAccountRequest;
import com.smart.mobility.smartmobilitybillingservice.dto.TripPricedEvent;
import com.smart.mobility.smartmobilitybillingservice.enums.TransactionStatus;
import com.smart.mobility.smartmobilitybillingservice.enums.TransactionType;
import com.smart.mobility.smartmobilitybillingservice.exception.AccountNotFoundException;
import com.smart.mobility.smartmobilitybillingservice.exception.DailyCapExceededException;
import com.smart.mobility.smartmobilitybillingservice.exception.InsufficientBalanceException;
import com.smart.mobility.smartmobilitybillingservice.messaging.PaymentEventPublisher;
import com.smart.mobility.smartmobilitybillingservice.model.Account;
import com.smart.mobility.smartmobilitybillingservice.model.Transaction;
import com.smart.mobility.smartmobilitybillingservice.repository.AccountRepository;
import com.smart.mobility.smartmobilitybillingservice.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BillingService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final PaymentEventPublisher eventPublisher;

    /**
     * Daily spending ceiling in XOF (or the account currency). Configurable per
     * environment.
     */
    @Value("${billing.daily-cap:50000}")
    private BigDecimal dailyCap;

    // ─────────────────────────────────────────────────────────────
    // 1. Create Account
    // ─────────────────────────────────────────────────────────────

    /**
     * Creates a new account for a user.
     * Called when a UserCreated event is received or via the REST endpoint.
     */
    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        if (accountRepository.existsByUserId(request.userId())) {
            log.warn("Account already exists for userId={}", request.userId());
            return getAccountByUserId(request.userId());
        }

        String currency = (request.currency() != null && !request.currency().isBlank())
                ? request.currency()
                : "XOF";

        Account account = Account.builder()
                .userId(request.userId())
                .balance(BigDecimal.ZERO)
                .dailySpent(BigDecimal.ZERO)
                .currency(currency)
                .build();

        account = accountRepository.save(account);
        log.info("Account created for userId={} with id={}", request.userId(), account.getId());
        return toResponse(account);
    }

    // ─────────────────────────────────────────────────────────────
    // 2. Top-Up (CREDIT)
    // ─────────────────────────────────────────────────────────────

    /**
     * Credits the account of the given user.
     *
     * @param userId      user identifier
     * @param amount      positive amount to add
     * @param description optional description
     * @return updated account information
     */
    @Transactional
    public AccountResponse topUp(Long userId, BigDecimal amount, String description) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Top-up amount must be positive.");
        }

        Account account = findAccountByUserId(userId);
        account.setBalance(account.getBalance().add(amount));
        account = accountRepository.save(account);

        saveTransaction(account.getId(), null, amount, TransactionType.CREDIT,
                TransactionStatus.SUCCESS, description != null ? description : "Account top-up");

        log.info("Top-up of {} credited to userId={}. New balance={}", amount, userId, account.getBalance());
        return toResponse(account);
    }

    // ─────────────────────────────────────────────────────────────
    // 3. Debit (triggered by TRIP_PRICED event)
    // ─────────────────────────────────────────────────────────────

    /**
     * Processes a TRIP_PRICED event:
     * <ol>
     * <li>Idempotence check — skip if already processed</li>
     * <li>Account existence check</li>
     * <li>Balance check</li>
     * <li>Daily cap check (trims amount if needed)</li>
     * <li>Debit the account</li>
     * <li>Publish PAYMENT_COMPLETED</li>
     * </ol>
     * On any business failure, records a FAILED transaction and publishes
     * PAYMENT_FAILED.
     */
    @Transactional
    public void processDebit(TripPricedEvent event) {
        log.info("Processing debit for tripId={}, userId={}, amount={}",
                event.tripId(), event.userId(), event.finalAmount());

        // ── Idempotence ──────────────────────────────────────────
        Optional<Transaction> existing = transactionRepository.findByTripId(event.tripId());
        if (existing.isPresent()) {
            log.warn("Duplicate TRIP_PRICED event for tripId={}. Skipping.", event.tripId());
            return;
        }

        // ── Account existence ─────────────────────────────────────
        Account account;
        try {
            account = findAccountByUserId(event.userId());
        } catch (AccountNotFoundException ex) {
            log.error("No account for userId={}", event.userId());
            saveTransaction(null, event.tripId(), event.finalAmount(), TransactionType.DEBIT,
                    TransactionStatus.FAILED, "Account not found for userId: " + event.userId());
            eventPublisher.publishPaymentFailed(event.tripId(), event.userId(),
                    event.finalAmount(), ex.getMessage());
            return;
        }

        BigDecimal amount = event.finalAmount();

        try {
            // ── Balance check ─────────────────────────────────────
            if (account.getBalance().compareTo(amount) < 0) {
                throw new InsufficientBalanceException(account.getBalance(), amount);
            }

            // ── Daily cap logic ───────────────────────────────────
            BigDecimal remaining = dailyCap.subtract(account.getDailySpent());
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                throw new DailyCapExceededException();
            }
            // Trim if capped
            if (account.getDailySpent().add(amount).compareTo(dailyCap) > 0) {
                log.info("Trimming amount {} to daily-cap remainder {} for userId={}",
                        amount, remaining, event.userId());
                amount = remaining;
            }

            // ── Debit ─────────────────────────────────────────────
            account.setBalance(account.getBalance().subtract(amount));
            account.setDailySpent(account.getDailySpent().add(amount));
            accountRepository.save(account);

            saveTransaction(account.getId(), event.tripId(), amount, TransactionType.DEBIT,
                    TransactionStatus.SUCCESS, "Trip payment for tripId: " + event.tripId());

            eventPublisher.publishPaymentCompleted(event.tripId(), event.userId(), amount);
            log.info("Debit of {} for tripId={} succeeded. Remaining balance={}",
                    amount, event.tripId(), account.getBalance());

        } catch (InsufficientBalanceException | DailyCapExceededException ex) {
            log.warn("Debit failed for tripId={}: {}", event.tripId(), ex.getMessage());
            saveTransaction(account.getId(), event.tripId(), event.finalAmount(), TransactionType.DEBIT,
                    TransactionStatus.FAILED, ex.getMessage());
            eventPublisher.publishPaymentFailed(event.tripId(), event.userId(),
                    event.finalAmount(), ex.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 4. Daily Cap Reset — every day at midnight
    // ─────────────────────────────────────────────────────────────

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void resetDailySpent() {
        log.info("Resetting dailySpent for all accounts...");
        accountRepository.findAll().forEach(account -> {
            account.setDailySpent(BigDecimal.ZERO);
            accountRepository.save(account);
        });
        log.info("Daily cap reset complete.");
    }

    // ─────────────────────────────────────────────────────────────
    // 5. Query
    // ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public AccountResponse getAccountByUserId(Long userId) {
        return toResponse(findAccountByUserId(userId));
    }

    // ─────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────

    private Account findAccountByUserId(Long userId) {
        return accountRepository.findByUserId(userId)
                .orElseThrow(() -> new AccountNotFoundException(userId));
    }

    private void saveTransaction(Long accountId, String tripId, BigDecimal amount,
            TransactionType type, TransactionStatus status,
            String description) {
        Transaction tx = Transaction.builder()
                .accountId(accountId)
                .tripId(tripId)
                .amount(amount)
                .type(type)
                .status(status)
                .description(description)
                .build();
        transactionRepository.save(tx);
    }

    private AccountResponse toResponse(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getUserId(),
                account.getBalance(),
                account.getDailySpent(),
                account.getCurrency());
    }
}
