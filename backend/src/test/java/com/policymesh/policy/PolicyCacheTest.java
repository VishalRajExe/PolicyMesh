package com.policymesh.policy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.policymesh.compiler.CompiledPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PolicyCacheTest {

  private ObjectMapper mapper;
  private StringRedisTemplate redisTemplate;
  private ObjectProvider<StringRedisTemplate> redisProvider;
  private ValueOperations<String, String> valueOperations;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    mapper = new ObjectMapper();
    redisTemplate = mock(StringRedisTemplate.class);
    redisProvider = mock(ObjectProvider.class);
    valueOperations = mock(ValueOperations.class);

    when(redisProvider.getIfAvailable()).thenReturn(redisTemplate);
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
  }

  @Test
  void cacheMissOnFirstLookupFollowedByCacheHitOnSecondLookup() {
    PolicyCache cache = new PolicyCache(redisProvider, mapper, true, 600);

    CompiledPolicy policy = new CompiledPolicy("EU-PII-001", "EU PII", "EU", "PII",
        Set.of("EU"), Set.of("US"), PolicyStatus.ACTIVE);
    List<CompiledPolicy> expectedPolicies = List.of(policy);

    AtomicInteger loaderCalls = new AtomicInteger(0);

    // 1. First lookup: should MISS and call loader
    List<CompiledPolicy> firstResult = cache.applicable("EU", "PII", () -> {
      loaderCalls.incrementAndGet();
      return expectedPolicies;
    });

    assertEquals(1, loaderCalls.get(), "Loader should be called exactly once on MISS");
    assertEquals(1, firstResult.size());
    assertEquals("EU-PII-001", firstResult.getFirst().policyCode());
    assertEquals(1, cache.getMisses());
    assertEquals(0, cache.getHits());

    // 2. Second lookup: should HIT fast-path / cache and NOT call loader
    List<CompiledPolicy> secondResult = cache.applicable("EU", "PII", () -> {
      loaderCalls.incrementAndGet();
      return List.of();
    });

    assertEquals(1, loaderCalls.get(), "Loader should NOT be called on cache HIT");
    assertEquals(1, secondResult.size());
    assertEquals("EU-PII-001", secondResult.getFirst().policyCode());
    assertEquals(1, cache.getMisses());
    assertEquals(1, cache.getHits());
  }

  @Test
  void cacheInvalidationForcesSubsequentMissAndReload() {
    PolicyCache cache = new PolicyCache(redisProvider, mapper, true, 600);

    CompiledPolicy initialPolicy = new CompiledPolicy("EU-PII-001", "EU PII", "EU", "PII",
        Set.of("EU"), Set.of("US"), PolicyStatus.ACTIVE);
    CompiledPolicy updatedPolicy = new CompiledPolicy("EU-PII-001", "EU PII Updated", "EU", "PII",
        Set.of("EU", "IN"), Set.of("US"), PolicyStatus.ACTIVE);

    AtomicInteger loaderCalls = new AtomicInteger(0);

    // Populate cache
    cache.applicable("EU", "PII", () -> {
      loaderCalls.incrementAndGet();
      return List.of(initialPolicy);
    });
    assertEquals(1, loaderCalls.get());

    // Invalidate cache (simulating policy update)
    cache.clear();
    assertTrue(cache.getEvictions() > 0);

    // Next lookup must reload from loader
    List<CompiledPolicy> freshResult = cache.applicable("EU", "PII", () -> {
      loaderCalls.incrementAndGet();
      return List.of(updatedPolicy);
    });

    assertEquals(2, loaderCalls.get(), "Loader should be called again after eviction");
    assertEquals(1, freshResult.size());
    assertEquals(Set.of("EU", "IN"), freshResult.getFirst().allowedRegions());
  }

  @Test
  void gracefulDegradationWhenRedisIsDisabledOrErrors() {
    // Simulate Redis throwing a runtime connection exception
    when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("Redis connection refused"));

    PolicyCache cache = new PolicyCache(redisProvider, mapper, true, 600);

    CompiledPolicy fallbackPolicy = new CompiledPolicy("GLOBAL-001", "Global Public", "GLOBAL", "PUBLIC",
        Set.of("GLOBAL"), Set.of(), PolicyStatus.ACTIVE);

    // Should NOT throw exception; must fall back to loader transparently
    List<CompiledPolicy> result = assertDoesNotThrow(() ->
        cache.applicable("GLOBAL", "PUBLIC", () -> List.of(fallbackPolicy))
    );

    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals("GLOBAL-001", result.getFirst().policyCode());
  }
}
