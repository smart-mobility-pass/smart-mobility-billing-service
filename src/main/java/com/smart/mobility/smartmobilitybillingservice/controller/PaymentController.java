package com.smart.mobility.smartmobilitybillingservice.controller;

import com.smart.mobility.smartmobilitybillingservice.model.Transaction;
import com.smart.mobility.smartmobilitybillingservice.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final TransactionRepository transactionRepository;

    @GetMapping("/{tripId}")
    public ResponseEntity<Transaction> getPaymentStatusByTripId(@PathVariable String tripId) {
        log.info("REST: Get payment status for tripId={}", tripId);
        Optional<Transaction> transaction = transactionRepository.findByTripId(tripId);

        return transaction
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
