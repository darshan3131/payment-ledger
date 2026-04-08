package com.darshan.payment_ledger.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

// OTP lifecycle: generate → store in Redis (5 min TTL) → verify → delete (one-time use)
// Key pattern: otp:{phone}  e.g. otp:+919876543210
// SecureRandom ensures cryptographically strong randomness — never use Math.random() for OTPs.

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private static final String OTP_PREFIX    = "otp:";
    private static final int    OTP_LENGTH    = 6;
    private static final long   OTP_TTL_MINS  = 5;

    @org.springframework.beans.factory.annotation.Value("${otp.dev-mode:true}")
    private boolean devMode;

    private static final String DEV_OTP = "123456";

    private final StringRedisTemplate redisTemplate;
    private final SecureRandom        secureRandom = new SecureRandom();

    // Generates a 6-digit OTP, stores it in Redis with 5-minute expiry, returns the OTP string.
    // DEV MODE: always uses 123456. Set otp.dev-mode=false in application.properties for production.
    public String generateAndStore(String phone) {
        String otp = devMode ? DEV_OTP : String.format("%06d", secureRandom.nextInt(1_000_000));
        if (devMode) log.warn("⚠️  DEV MODE — OTP hardcoded to {} for all requests", DEV_OTP);
        redisTemplate.opsForValue().set(
            OTP_PREFIX + phone,
            otp,
            Duration.ofMinutes(OTP_TTL_MINS)
        );
        log.info("OTP generated for phone ending in ...{}", phone.length() > 4 ? phone.substring(phone.length() - 4) : "****");
        return otp;
    }

    // Returns true if the provided OTP matches what's stored and deletes it immediately (one-time use).
    // Returns false if expired or wrong.
    public boolean verifyAndConsume(String phone, String otp) {
        String key    = OTP_PREFIX + phone;
        String stored = redisTemplate.opsForValue().get(key);
        if (stored == null) {
            log.warn("OTP verify failed: no OTP found for phone (expired or not generated)");
            return false;
        }
        if (!stored.equals(otp)) {
            log.warn("OTP verify failed: wrong OTP for phone ending ...{}", phone.length() > 4 ? phone.substring(phone.length() - 4) : "****");
            return false;
        }
        redisTemplate.delete(key);   // consume: one-time use
        return true;
    }

    // Explicitly invalidate an OTP (e.g., after too many wrong attempts).
    public void invalidate(String phone) {
        redisTemplate.delete(OTP_PREFIX + phone);
    }
}
