package com.policymesh.policy;

import com.policymesh.compiler.CompiledPolicy;

import java.util.List;
import java.util.function.Supplier;

/**
 * Cache abstraction for compiled policies backed by Redis (or resilient in-memory fallback).
 */
public interface PolicyCacheService {

  /**
   * Retrieves compiled policies for the given jurisdiction and data class, or loads from database.
   */
  List<CompiledPolicy> applicable(String jurisdiction, String dataClass, Supplier<List<CompiledPolicy>> loader);

  /**
   * Directly stores compiled policies into cache.
   */
  void putCompiledPolicy(String jurisdiction, String dataClass, List<CompiledPolicy> policies);

  /**
   * Directly fetches compiled policies from cache if present.
   */
  List<CompiledPolicy> getCompiledPolicy(String jurisdiction, String dataClass);

  /**
   * Evicts a specific (jurisdiction, dataClass) entry.
   */
  void evictPolicy(String jurisdiction, String dataClass);

  /**
   * Evicts all cached policy entries on policy create/update/delete.
   */
  void clear();

  long getHits();

  long getMisses();

  long getEvictions();
}
