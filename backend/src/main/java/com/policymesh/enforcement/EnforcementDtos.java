package com.policymesh.enforcement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class EnforcementDtos {
  private EnforcementDtos() {}

  /**
   * Two documented shapes are accepted: the full runtime shape with regions
   * (sourceService/destinationService/sourceRegion/destinationRegion/dataClassTags) and the
   * minimal shape (source/destination/dataClass). Missing regions are resolved from the
   * registered service graph when possible.
   */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record Request(String sourceService, String destinationService,
                        String sourceRegion, String destinationRegion,
                        Set<String> dataClassTags,
                        Set<String> dataClasses,
                        String source, String destination, String dataClass) {

    public static Request of(String sourceService, String destinationService,
                             String sourceRegion, String destinationRegion, Set<String> dataClassTags) {
      return new Request(sourceService, destinationService, sourceRegion, destinationRegion, dataClassTags, null, null, null, null);
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
