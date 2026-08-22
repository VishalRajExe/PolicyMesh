package com.policymesh.policy;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.policymesh.compiler.CompiledPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * High-performance, resilient cache for compiled ACTIVE policy sets backed by Redis with local fallback.
 * Database (MySQL / PostgreSQL) remains the authoritative source of truth.
 */
@Service
public class PolicyCache implements PolicyCacheService {
  private static final Logger log = LoggerFactory.getLogger(PolicyCache.class);
  public static final String KEY_PREFIX = "policymesh:policy:compiled:";

  private final ObjectProvider<StringRedisTemplate> redis;
  private final ObjectMapper mapper;
  private final boolean enabled;
  private final Duration ttl;
  private final Map<String, List<CompiledPolicy>> local = new ConcurrentHashMap<>();

  private final AtomicLong hits = new AtomicLong();
  private final AtomicLong misses = new AtomicLong();
  private final AtomicLong evictions = new AtomicLong();
  private final AtomicLong errors = new AtomicLong();

  public PolicyCache(ObjectProvider<StringRedisTemplate> redis,
                     ObjectMapper mapper,
                     @Value("${policymesh.redis.enabled:true}") boolean enabled,
                     @Value("${policymesh.redis.ttl-seconds:${REDIS_POLICY_TTL_SECONDS:600}}") long ttlSeconds) {
    this.redis = redis;
    this.mapper = mapper;
    this.enabled = enabled;
    this.ttl = Duration.ofSeconds(ttlSeconds);
  }

  @Override
  public List<CompiledPolicy> applicable(String jurisdiction, String dataClass, Supplier<List<CompiledPolicy>> loader) {
    String key = buildKey(jurisdiction, dataClass);

    // 1. Check local fast-path
    List<CompiledPolicy> cached = local.get(key);
    if (cached != null) {
      hits.incrementAndGet();
      log.info("Redis HIT (local fast-path): {}", key);
      return cached;
    }

    // 2. Check Redis
    cached = fromRedis(key);
    if (cached != null) {
      hits.incrementAndGet();
      local.put(key, cached);
      log.info("Redis HIT: {}", key);
      return cached;
    }

    // 3. Cache MISS -> Load from database source of truth
    misses.incrementAndGet();
    log.info("Redis MISS: {} (fetching from database)", key);
    List<CompiledPolicy> loaded = loader.get();
    local.put(key, loaded);
    toRedis(key, loaded);
    return loaded;
  }

  @Override
  public void putCompiledPolicy(String jurisdiction, String dataClass, List<CompiledPolicy> policies) {
    String key = buildKey(jurisdiction, dataClass);
    local.put(key, policies);
    toRedis(key, policies);
  }

  @Override
  public List<CompiledPolicy> getCompiledPolicy(String jurisdiction, String dataClass) {
    String key = buildKey(jurisdiction, dataClass);
    List<CompiledPolicy> cached = local.get(key);
    if (cached != null) return cached;
    return fromRedis(key);
  }

  @Override
  public void evictPolicy(String jurisdiction, String dataClass) {
    String key = buildKey(jurisdiction, dataClass);
    local.remove(key);
    evictions.incrementAndGet();
    if (!enabled) return;
    try {
      StringRedisTemplate template = redis.getIfAvailable();
      if (template != null) {
        template.delete(key);
        log.info("Redis EVICT: {}", key);
      }
    } catch (RuntimeException e) {
      errors.incrementAndGet();
      log.warn("Redis eviction failed ({}); cache will expire via TTL", e.getMessage());
    }
  }

  @Override
  public void clear() {
    local.clear();
    evictions.incrementAndGet();
    if (!enabled) return;
    try {
      StringRedisTemplate template = redis.getIfAvailable();
      if (template != null) {
        var keys = template.keys(KEY_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
          template.delete(keys);
          log.info("Redis EVICT: cleared {} cached policy keys", keys.size());
        }
      }
    } catch (RuntimeException e) {
      errors.incrementAndGet();
      log.warn("Redis clear failed ({}); cache will expire via TTL", e.getMessage());
    }
  }

  @Override
  public long getHits() { return hits.get(); }

  @Override
  public long getMisses() { return misses.get(); }

  @Override
  public long getEvictions() { return evictions.get(); }

  public long getErrors() { return errors.get(); }

  private String buildKey(String jurisdiction, String dataClass) {
    return KEY_PREFIX + (jurisdiction == null ? "GLOBAL" : jurisdiction.trim().toUpperCase())
        + ":" + (dataClass == null ? "ANY" : dataClass.trim().toUpperCase());
  }

  private List<CompiledPolicy> fromRedis(String key) {
    if (!enabled) return null;
    try {
      StringRedisTemplate template = redis.getIfAvailable();
      if (template == null) return null;
      String raw = template.opsForValue().get(key);
      if (raw == null) return null;
      return mapper.readValue(raw, new TypeReference<List<CompiledPolicy>>() {});
    } catch (RuntimeException | java.io.IOException e) {
      errors.incrementAndGet();
      log.warn("Redis lookup failed for key {} ({}); falling back to database", key, e.getMessage());
      return null;
    }
  }

  private void toRedis(String key, List<CompiledPolicy> policies) {
    if (!enabled || policies == null) return;
    try {
      StringRedisTemplate template = redis.getIfAvailable();
      if (template != null) {
        String json = mapper.writeValueAsString(policies);
        template.opsForValue().set(key, json, ttl);
        log.info("Redis SET: {} (TTL: {}s, {} policies)", key, ttl.toSeconds(), policies.size());
      }
    } catch (RuntimeException | java.io.IOException e) {
      errors.incrementAndGet();
      log.warn("Redis write failed for key {} ({}); continuing with in-memory value", key, e.getMessage());
    }
  }
}
