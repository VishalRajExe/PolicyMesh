package com.policymesh.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(columnNames = "email"))
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String email;

  @Column(nullable = false)
  private String passwordHash;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 64)
  private Role role = Role.ENGINEER;

  @Column(nullable = false, length = 50)
  private String status = "ACTIVE";

  @Column(name = "enabled", nullable = false)
  private Boolean enabled = true;

  @Column(name = "name")
  private String name;

  @Column(nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", columnDefinition = "DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6)")
  private Instant updatedAt = Instant.now();

  @jakarta.persistence.PrePersist
  protected void onCreate() {
    if (createdAt == null) createdAt = Instant.now();
    if (updatedAt == null) updatedAt = Instant.now();
    if (enabled == null) enabled = true;
    if (status == null) status = "ACTIVE";
  }

  @jakarta.persistence.PreUpdate
  protected void onUpdate() {
    updatedAt = Instant.now();
  }

  public User() {}

  public User(String email, String passwordHash, Role role) {
    this.email = email;
    this.passwordHash = passwordHash;
    this.role = role != null ? role : Role.ENGINEER;
    this.status = "ACTIVE";
    this.enabled = true;
    this.createdAt = Instant.now();
    this.updatedAt = Instant.now();
  }

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }

  public String getEmail() { return email; }
  public void setEmail(String email) { this.email = email; }

  public String getPasswordHash() { return passwordHash; }
  public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

  public Role getRole() { return role; }
  public void setRole(Role role) { this.role = role; }

  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }

  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

  public Instant getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

  public Boolean isEnabled() { return enabled != null ? enabled : true; }
  public void setEnabled(Boolean enabled) { this.enabled = enabled != null ? enabled : true; }

  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
}
