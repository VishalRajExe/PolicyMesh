package com.policymesh.ai;

/**
 * Abstraction over the classification backend: an external AI service, a local heuristic
 * implementation, or the local one acting as fallback when the remote service is unreachable.
 */
public interface ClassificationProvider {
  Result classify(String fieldName, String sampleValue);

  String describe();

  record Result(String classification, double confidence, String provider) {}
}
