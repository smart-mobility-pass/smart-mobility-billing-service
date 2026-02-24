package com.smart.mobility.smartmobilitybillingservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.PAYMENT_REQUIRED)
public class InsufficientBalanceException extends RuntimeException {

    public InsufficientBalanceException(java.math.BigDecimal balance, java.math.BigDecimal required) {
        super(String.format("Insufficient balance: available %.2f, required %.2f", balance, required));
    }
}
