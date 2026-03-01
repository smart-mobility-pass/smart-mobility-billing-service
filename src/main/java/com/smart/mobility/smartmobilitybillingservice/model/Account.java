package com.smart.mobility.smartmobilitybillingservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "accounts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Unique identifier of the user owning this account. */
    @Column(nullable = false, unique = true)
    private String userId;

    /** Current balance. Must never be negative. */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    /** Amount already spent today — reset daily by a cron job. */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal dailySpent;

    /**
     * ISO 4217 currency code. Default is XOF (West-African CFA franc).
     */
    @Column(nullable = false, length = 3)
    private String currency;

    /** Timestamp of the last modification. */
    private LocalDateTime updatedAt;

    /**
     * Optimistic locking: prevents concurrent debit/credit conflicts.
     * JPA increments this automatically on each flush that modifies the row.
     */
    @Version
    private Long version;

    @PrePersist
    @PreUpdate
    public void updateTimestamp() {
        this.updatedAt = LocalDateTime.now();
    }
}
