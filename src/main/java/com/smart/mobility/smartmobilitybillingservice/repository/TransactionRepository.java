package com.smart.mobility.smartmobilitybillingservice.repository;

import com.smart.mobility.smartmobilitybillingservice.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    /** Used for idempotence: check if a trip was already processed. */
    Optional<Transaction> findByTripId(String tripId);

    /** Retrieve all transactions for an account (for statements, etc.) */
    List<Transaction> findByAccountIdOrderByCreatedAtDesc(Long accountId);
}
