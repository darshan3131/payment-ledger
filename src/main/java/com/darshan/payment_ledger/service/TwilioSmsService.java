package com.darshan.payment_ledger.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;

// Twilio SMS via REST API — no SDK dependency needed.
// Uses Basic Auth: Base64("accountSid:authToken")
// Docs: https://www.twilio.com/docs/sms/api/message-resource
//
// DEV MODE: if twilio.account-sid is blank/unset, OTP is printed to the console instead.
// This lets you develop and test the full OTP flow without a Twilio account.
// For production: set the three twilio.* properties in application.properties (or env vars).

@Service
@Slf4j
public class TwilioSmsService implements SmsService {

    @Value("${twilio.account-sid:}")
    private String accountSid;

    @Value("${twilio.auth-token:}")
    private String authToken;

    @Value("${twilio.from-number:}")
    private String fromNumber;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public void send(String toPhone, String message) {
        // DEV FALLBACK: if Twilio is not configured, log OTP to console
        if (accountSid == null || accountSid.isBlank()) {
            log.warn("=======================================================");
            log.warn("  TWILIO NOT CONFIGURED — DEV MODE OTP");
            log.warn("  To: {}", toPhone);
            log.warn("  Message: {}", message);
            log.warn("  Set twilio.account-sid, twilio.auth-token, twilio.from-number");
            log.warn("=======================================================");
            return;
        }

        // DEV CONSOLE LOG — always print OTP so you can test without SMS delivery
        log.warn("╔══════════════════════════════════════════════╗");
        log.warn("║  OTP MESSAGE (dev console fallback)          ║");
        log.warn("║  To: {}  ║", toPhone);
        log.warn("║  Body: {}  ║", message);
        log.warn("╚══════════════════════════════════════════════╝");

        try {
            String url = "https://api.twilio.com/2010-04-01/Accounts/" + accountSid + "/Messages.json";

            // Basic Auth header
            String credentials = accountSid + ":" + authToken;
            String encoded     = Base64.getEncoder().encodeToString(credentials.getBytes());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("Authorization", "Basic " + encoded);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("To",   toPhone);
            body.add("From", fromNumber);
            body.add("Body", message);

            restTemplate.postForObject(url, new HttpEntity<>(body, headers), String.class);
            log.info("SMS sent via Twilio to ...{}", toPhone.length() > 4 ? toPhone.substring(toPhone.length() - 4) : "****");

        } catch (Exception e) {
            // Log but never rethrow — a failed SMS must not 500 the caller.
            // OTP is already stored in Redis; user can tap "Resend" if they don't receive it.
            log.error("Twilio SMS delivery failed to ...{}: {}",
                    toPhone.length() > 4 ? toPhone.substring(toPhone.length() - 4) : "****",
                    e.getMessage());
        }
    }
}
