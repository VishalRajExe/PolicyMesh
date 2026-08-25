package com.policymesh.policy;
import jakarta.persistence.*; import java.time.Instant; import java.util.*;
@Entity
@Table(
    name = "policies",
    uniqueConstraints = @UniqueConstraint(columnNames = "policy_code"),
    indexes = @Index(name = "idx_policy_data_class", columnList = "data_class")
)
public class Policy {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "policy_code", nullable = false)
  private String policyCode;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private String jurisdiction;

  @Column(name = "data_class", nullable = false)
  private String dataClass;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "policy_allowed_regions", joinColumns = @JoinColumn(name = "policy_id"))
  @Column(name = "region")
  private Set<String> allowedRegions = new TreeSet<>();

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "policy_denied_regions", joinColumns = @JoinColumn(name = "policy_id"))
  @Column(name = "region")
  private Set<String> deniedRegions = new TreeSet<>();

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private PolicyStatus status = PolicyStatus.DRAFT;

  @Column(nullable = false)
  private int version = 1;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  @PreUpdate
  void updated() { updatedAt = Instant.now(); }

  public Long getId() { return id; }
  public String getPolicyCode() { return policyCode; }
  public String getName() { return name; }
  public String getJurisdiction() { return jurisdiction; }
  public String getDataClass() { return dataClass; }
  public Set<String> getAllowedRegions() { return allowedRegions; }
  public Set<String> getDeniedRegions() { return deniedRegions; }
  public PolicyStatus getStatus() { return status; }
  public int getVersion() { return version; }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }

  public void setPolicyCode(String v) { policyCode = v; }
  public void setName(String v) { name = v; }
  public void setJurisdiction(String v) { jurisdiction = v; }
  public void setDataClass(String v) { dataClass = v; }
  public void setAllowedRegions(Set<String> v) { allowedRegions = v; }
  public void setDeniedRegions(Set<String> v) { deniedRegions = v; }
  public void setStatus(PolicyStatus v) { status = v; }
  public void setVersion(int v) { version = v; }
}
