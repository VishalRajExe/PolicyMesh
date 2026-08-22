package com.policymesh.policy;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Canonical vocabulary for regions, jurisdictions and data classes.
 * The recognized region set is intentionally open (configuration, not hard-coded policy law);
 * aliases fold synonymous spellings onto one canonical token so that e.g. a policy authored
 * with jurisdiction INDIA applies to services registered in region IN.
 */
public final class PolicyVocabulary {
  private PolicyVocabulary() {}

  public static final String GLOBAL_JURISDICTION = "GLOBAL";
  public static final String PUBLIC_DATA_CLASS = "PUBLIC";

  /** Data classes a policy may reference. */
  public static final Set<String> DATA_CLASSES = Set.of("PII", "PCI", "PHI", "PUBLIC", "NON_SENSITIVE", "UNKNOWN");

  private static final Map<String, String> REGION_ALIASES = Map.of(
      "INDIA", "IN",
      "EUROPE", "EU",
      "USA", "US",
      "UK", "GB",
      "GREAT_BRITAIN", "GB");

  public static String canonicalRegion(String region) {
    if (region == null) return "";
    String upper = region.trim().toUpperCase(Locale.ROOT);
    return REGION_ALIASES.getOrDefault(upper, upper);
  }

  public static String canonicalDataClass(String dataClass) {
    if (dataClass == null) return "";
    String upper = dataClass.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    return "NONSENSITIVE".equals(upper) ? "NON_SENSITIVE" : upper;
  }

  public static boolean isKnownDataClass(String dataClass) {
    return DATA_CLASSES.contains(canonicalDataClass(dataClass));
  }
}
