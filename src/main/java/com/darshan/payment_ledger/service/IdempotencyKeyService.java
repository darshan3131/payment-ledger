package com.darshan.payment_ledger.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

// Single-responsibility: owns all Redis operations for idempotency keys.
// TransactionService calls this instead of hitting MySQL directly.
@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyKeyService {

    private final RedisTemplate<String, String> redisTemplate;

    // Key format: "idempotency:{clientKey}"
    // Keeps Redis keys namespaced — avoids collisions with other future keys.
    private static final String PREFIX = "idempotency:";
    private static final long TTL_HOURS = 24;

    // Save: store key → referenceId with 24-hour expiry.
    // Redis auto-deletes it after TTL. No cleanup job needed.
    public void save(String idempotencyKey, String referenceId) {
        String redisKey = PREFIX + idempotencyKey;
        redisTemplate.opsForValue().set(redisKey, referenceId, TTL_HOURS, TimeUnit.HOURS);
        log.debug("Saved idempotency key to Redis: {} → {}", redisKey, referenceId);
    }

    // Lookup: returns referenceId if key exists, null if not found or expired.
    public String findReferenceId(String idempotencyKey) {
        return redisTemplate.opsForValue().get(PREFIX + idempotencyKey);
    }

    // Check: simple boolean — does this key already exist?
    public boolean exists(String idempotencyKey) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(PREFIX + idempotencyKey));
    }
}