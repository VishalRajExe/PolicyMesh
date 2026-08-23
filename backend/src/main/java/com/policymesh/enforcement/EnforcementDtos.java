package com.policymesh.enforcement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.HashSet;
import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class EnforcementDtos {
  private EnforcementDtos() {}

  /**
   * Accepted request shapes:
   * 1. Full runtime shape with regions and dataClassTags (sourceService/destService/regions/dataClassTags)
   * 2. Schema field shape (sourceService/destService/regions/fields) where fields resolve against approved AI classifications
   * 3. Minimal shape (source/destination/dataClass)
   */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record Request(String sourceService, String destinationService,
                        String sourceRegion, String destinationRegion,
                        Set<String> dataClassTags,
                        Set<String> dataClasses,
                        String source, String destination, String dataClass,
                        Set<String> fields,
                        Set<String> schemaFields,
                        String fieldName) {

    public static Request of(String sourceService, String destinationService,
                             String sourceRegion, String destinationRegion, Set<String> dataClassTags) {
      return new Request(sourceService, destinationService, sourceRegion, destinationRegion, dataClassTags,
                         null, null, null, null, null, null, null);
    }

    public String effectiveSource() { return firstNonBlank(sourceService, source); }
    public String effectiveDestination() { return firstNonBlank(destinationService, destination); }
    public String effectiveSourceRegion() { return blankToNull(sourceRegion); }
    public String effectiveDestinationRegion() { return blankToNull(destinationRegion); }

    public Set<String> effectiveTags() {
      if (dataClassTags != null && !dataClassTags.isEmpty()) return dataClassTags;
      if (dataClasses != null && !dataClasses.isEmpty()) return dataClasses;
      if (dataClass != null && !dataClass.isBlank()) return Set.of(dataClass);
      return Set.of();
    }

    public Set<String> effectiveFields() {
      Set<String> set = new HashSet<>();
      if (fields != null) set.addAll(fields);
      if (schemaFields != null) set.addAll(schemaFields);
      if (fieldName != null && !fieldName.isBlank()) set.add(fieldName);
      return set;
    }

    private static String firstNonBlank(String a, String b) {
      return a != null && !a.isBlank() ? a : b;
    }

    private static String blankToNull(String s) {
      return s == null || s.isBlank() ? null : s;
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record Response(String decision, String policyId, String reason,
                         Long decisionId, Long lineageId, String lineageHash) {}
}
