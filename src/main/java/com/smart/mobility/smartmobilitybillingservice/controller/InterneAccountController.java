package com.smart.mobility.smartmobilitybillingservice.controller;

import com.smart.mobility.smartmobilitybillingservice.dto.DailySpentResponse;
import com.smart.mobility.smartmobilitybillingservice.service.BillingService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/accounts")
public class InterneAccountController {

    private final BillingService billingService;

    /**
     * GET /internal/accounts/daily-spent/{userId}
     */
    @GetMapping("/daily-spent/{userId}")
    public ResponseEntity<DailySpentResponse> getDailySpent(@PathVariable String userId) {
        return ResponseEntity.ok(billingService.getDailySpent(userId));
    }
}
