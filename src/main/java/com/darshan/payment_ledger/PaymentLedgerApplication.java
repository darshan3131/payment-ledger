package com.darshan.payment_ledger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

// @EnableScheduling activates Spring's scheduled task executor.
// Without this annotation, any @Scheduled method in the app is silently ignored.
@SpringBootApplication
@EnableScheduling
public class PaymentLedgerApplication {

	public static void main(String[] args) {
		SpringApplication.run(PaymentLedgerApplication.class, args);
	}

}
