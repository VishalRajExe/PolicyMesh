package com.policymesh.policy;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.policymesh.compiler.CompiledPolicy;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Cache of the compiled ACTIVE policy set per (jurisdiction, dataClass), per docs/REDIS.md.
 * Redis is a pure accelerator: an outage or serialization hiccup is indistinguishable from a
 * cache miss and the loader (PostgreSQL) answers instead. No compliance decision ever
 * depends on this cache being reachable.
 */
@Component
public class PolicyCache {
  private static final String KEY_PREFIX = "policymesh:policy:";
  private static final Duration TTL = Duration.ofMinutes(10);

  private final ObjectProvider<StringRedisTemplate> redis;
  private final ObjectMapper mapper;
  private final boolean enabled;
  private final Map<String, List<CompiledPolicy>> local = new ConcurrentHashMap<>();

  public PolicyCache(ObjectProvider<StringRedisTemplate> redis,
                     ObjectMapper mapper,
                     @Value("${policymesh.redis.enabled:true}") boolean enabled) {
    this.redis = redis;
    this.mapper = mapper;
    this.enabled = enabled;
  }

  public List<CompiledPolicy> applicable(String jurisdiction, String dataClass, Supplier<List<CompiledPolicy>> loader) {
    String key = KEY_PREFIX + jurisdiction + ":" + dataClass;
    List<CompiledPolicy> cached = local.get(key);
    if (cached != null) return cached;
    cached = fromRedis(key);
    if (cached != null) {
      local.put(key, cached);
      return cached;
    }
    List<CompiledPolicy> loaded = loader.get();
    local.put(key, loaded);
    toRedis(key, loaded);
    return loaded;
  }

  /** Called on every policy create/update/delete so stale compiled sets are never served. */
  public void clear() {
    local.clear();
    if (!enabled) return;
    try {
      StringRedisTemplate template = redis.getIfAvailable();
      if (template != null) {
        var keys = template.keys(KEY_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) template.delete(keys);
      }
    } catch (RuntimeException ignored) {
      // cache cleanup is best-effort; entries expire via TTL anyway
    }
  }

  private List<CompiledPolicy> fromRedis(String key) {
    if (!enabled) return null;
    try {
      StringRedisTemplate template = redis.getIfAvailable();
      if (template == null) return null;
      String raw = template.opsForValue().get(key);
      return raw == null ? null : mapper.readValue(raw, new TypeReference<List<CompiledPolicy>>() {});
    } catch (RuntimeException | java.io.IOException ignored) {
      return null;
    }
  }

  private void toRedis(String key, List<CompiledPolicy> policies) {
    if (!enabled) return;
    try {
      StringRedisTemplate template = redis.getIfAvailable();
      if (template != null) template.opsForValue().set(key, mapper.writeValueAsString(policies), TTL);
    } catch (RuntimeException | java.io.IOException ignored) {
      // best-effort only
    }
  }
}
