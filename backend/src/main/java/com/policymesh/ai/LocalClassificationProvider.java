package com.policymesh.ai;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;

/** Deterministic offline classifier used by default and as fallback when the remote AI service is down. */
@Component
public class LocalClassificationProvider implements ClassificationProvider {
  private static final Map<String, String> EXACT_RULES = Map.ofEntries(
      Map.entry("email", "PII"),
      Map.entry("emailaddress", "PII"),
      Map.entry("cardnumber", "PCI"),
      Map.entry("creditcard", "PCI"),
      Map.entry("pan", "PCI"),
      Map.entry("cvv", "PCI"),
      Map.entry("phone", "PII"),
      Map.entry("phonenumber", "PII"),
      Map.entry("orderid", "NON_SENSITIVE"),
      Map.entry("ssn", "PII"),
      Map.entry("aadharnumber", "PII"),
      Map.entry("diagnosis", "PHI"),
      Map.entry("medicalrecord", "PHI"));

  @Override
  public Result classify(String fieldName, String sampleValue) {
    String haystack = ((fieldName == null ? "" : fieldName) + " " + (sampleValue == null ? "" : sampleValue))
        .toLowerCase(Locale.ROOT).trim();
    String normalized = haystack.replaceAll("[^a-z0-9]", "");
    for (Map.Entry<String, String> rule : EXACT_RULES.entrySet()) {
      if (normalized.contains(rule.getKey())) {
        return new Result(rule.getValue(), 0.85, "local");
      }
    }
    if (haystack.contains("@")) return new Result("PII", 0.85, "local");
    if (haystack.matches(".*\\b\\d{13,19}\\b.*")) return new Result("PCI", 0.80, "local");
    return new Result("NON_SENSITIVE", 0.55, "local");
  }

  @Override
  public String describe() { return "local heuristic classifier"; }
}
