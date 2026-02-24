package com.smart.mobility.smartmobilitybillingservice.controller;

import com.smart.mobility.smartmobilitybillingservice.dto.AccountResponse;
import com.smart.mobility.smartmobilitybillingservice.dto.CreateAccountRequest;
import com.smart.mobility.smartmobilitybillingservice.dto.TopUpRequest;
import com.smart.mobility.smartmobilitybillingservice.service.BillingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
@Slf4j
public class AccountController {

    private final BillingService billingService;

    /**
     * POST /accounts
     * Creates a new account for a user (called from User Service or admin).
     */
    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(@RequestBody @Valid CreateAccountRequest request) {
        log.info("REST: Create account for userId={}", request.userId());
        AccountResponse response = billingService.createAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /accounts/{userId}
     * Retrieves the account details for a user.
     */
    @GetMapping("/{userId}")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable Long userId) {
        log.info("REST: Get account for userId={}", userId);
        return ResponseEntity.ok(billingService.getAccountByUserId(userId));
    }

    /**
     * POST /accounts/{userId}/topup
     * Credits the account with a positive amount.
     */
    @PostMapping("/{userId}/topup")
    public ResponseEntity<AccountResponse> topUp(
            @PathVariable Long userId,
            @RequestBody TopUpRequest request) {
        log.info("REST: Top-up {} for userId={}", request.amount(), userId);
        AccountResponse response = billingService.topUp(userId, request.amount(), request.description());
        return ResponseEntity.ok(response);
    }
}
