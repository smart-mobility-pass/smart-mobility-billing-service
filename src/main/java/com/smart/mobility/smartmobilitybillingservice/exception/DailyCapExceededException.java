package com.smart.mobility.smartmobilitybillingservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class DailyCapExceededException extends RuntimeException {

    public DailyCapExceededException() {
        super("Daily spending cap has been reached. No further transactions allowed today.");
    }

    public DailyCapExceededException(String message) {
        super(message);
    }
}
