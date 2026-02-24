package com.smart.mobility.smartmobilitybillingservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
/*** @EnableScheduling allows us to use @Scheduled annotations for periodic tasks,
 * such as resetting daily spent amounts at midnight.
 */
@EnableScheduling
public class SmartMobilityBillingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartMobilityBillingServiceApplication.class, args);
    }
}
