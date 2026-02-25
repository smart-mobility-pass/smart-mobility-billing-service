package com.smart.mobility.smartmobilitybillingservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SmartMobilityBillingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartMobilityBillingServiceApplication.class, args);
    }
}
