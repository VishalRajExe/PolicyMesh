package com.policymesh.policy.service;

import com.policymesh.compiler.CompiledPolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * Caches compiled policies in Redis keyed as "policy:{jurisdiction}:{dataClass}".
 * PostgreSQL remains the source of truth; a cache miss or a Redis outage
 * simply falls back to loading + compiling from the database.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PolicyCacheService {

    private static final Duration TTL = Duration.ofMinutes(10);

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${policymesh.redis.enabled:true}")
    private boolean redisEnabled;

    private String key(String jurisdiction, String dataClass) {
        return "policy:" + jurisdiction + ":" + dataClass;
    }

    public Optional<CompiledPolicy> get(String jurisdiction, String dataClass) {
        if (!redisEnabled) {
            return Optional.empty();
        }
        try {
            Object cached = redisTemplate.opsForValue().get(key(jurisdiction, dataClass));
            if (cached instanceof CompiledPolicy compiledPolicy) {
                return Optional.of(compiledPolicy);
            }
            return Optional.empty();
        } catch (Exception ex) {
            log.warn("Redis unavailable during policy cache read, falling back to DB: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    public void put(String jurisdiction, String dataClass, CompiledPolicy policy) {
        if (!redisEnabled) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(key(jurisdiction, dataClass), policy, TTL);
        } catch (Exception ex) {
            log.warn("Redis unavailable during policy cache write, continuing without cache: {}", ex.getMessage());
        }
    }

    public void evictAll() {
        if (!redisEnabled) {
            return;
        }
        try {
            var keys = redisTemplate.keys("policy:*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception ex) {
            log.warn("Redis unavailable during cache eviction: {}", ex.getMessage());
        }
    }
}
