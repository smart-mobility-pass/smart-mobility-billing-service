package com.smart.mobility.smartmobilitybillingservice.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private Long id;

    //ID of the user associated with this account
    @Column(nullable = false)
    private Long userId;
    @Column(nullable = false, precision = 15, scale = 2)
    private Double balance;
    @Column(nullable = false, precision = 15, scale = 2)
    private Double dailySpent;
    @Column(nullable = false)
    private String currency;

    private LocalDateTime updatedAt;

    /// To documentate @PrePersist and @PreUpdate,
    ///  these annotations are used to automatically update the timestamp of the last update to the account.
    ///  Whenever a new account is created or an existing account is updated,
    /// the updateTimestamp method will be called, setting the updatedAt field to the current date and time.
    ///  This allows us to keep track of when the account was last modified.
    @PrePersist
    @PreUpdate
    public void updateTimestamp() {
        this.updatedAt = LocalDateTime.now();
    }

    public Account() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Double getBalance() {
        return balance;
    }

    public void setBalance(Double balance) {
        this.balance = balance;
    }

    public Double getDailySpent() {
        return dailySpent;
    }

    public void setDailySpent(Double dailySpent) {
        this.dailySpent = dailySpent;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
