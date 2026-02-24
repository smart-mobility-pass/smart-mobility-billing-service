package com.smart.mobility.smartmobilitybillingservice.model;

import com.smart.mobility.smartmobilitybillingservice.enums.TransactionStatus;
import com.smart.mobility.smartmobilitybillingservice.enums.TransactionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_transaction_trip_id", columnList = "tripId", unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Foreign key to the Account. */
    @Column(nullable = false)
    private Long accountId;

    /**
     * The trip that triggered this transaction.
     * Unique constraint ensures idempotence: one debit per trip.
     */
    @Column(unique = true)
    private String tripId;

    /** Amount involved in this transaction. Always positive. */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    /** DEBIT or CREDIT. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    /** SUCCESS, FAILED, or PENDING. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;

    /** Human-readable description of the transaction. */
    private String description;

    /** Timestamp when the transaction was recorded. */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
