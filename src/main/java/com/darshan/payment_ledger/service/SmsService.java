package com.darshan.payment_ledger.service;

// Abstraction over any SMS provider (Twilio, Fast2SMS, MSG91, etc.)
// Swap the implementation bean without touching any other code.
public interface SmsService {

    // Sends a plain-text SMS to the given phone number (E.164 format: +91XXXXXXXXXX).
    // Throws RuntimeException on delivery failure.
    void send(String toPhone, String message);
}
