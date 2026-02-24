package com.smart.mobility.smartmobilitybillingservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(Long userId) {
        super("Account not found for userId: " + userId);
    }

    public AccountNotFoundException(String message) {
        super(message);
    }
}
