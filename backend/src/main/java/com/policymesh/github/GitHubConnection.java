package com.policymesh.github;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(
    name = "github_connections",
    indexes = {
        @Index(name = "idx_github_conn_user_id", columnList = "userId", unique = true),
        @Index(name = "idx_github_conn_username", columnList = "githubUsername")
    }
)
public class GitHubConnection {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private Long userId;

  private Long githubUserId;

  @Column(nullable = false, length = 150)
  private String githubUsername;

  @Column(length = 255)
  private String githubEmail;

  @Column(length = 500)
  private String avatarUrl;

  @Column(nullable = false, length = 1000)
  private String encryptedAccessToken;

  @Column(length = 50)
  private String tokenType = "bearer";

  @Column(length = 255)
  private String scope;

  @Column(nullable = false)
  private Instant connectedAt = Instant.now();

  @Column(nullable = false)
  private Instant updatedAt = Instant.now();

  public GitHubConnection() {}

  public GitHubConnection(Long userId, Long githubUserId, String githubUsername, String githubEmail,
                          String avatarUrl, String encryptedAccessToken, String tokenType, String scope) {
    this.userId = userId;
    this.githubUserId = githubUserId;
    this.githubUsername = githubUsername;
    this.githubEmail = githubEmail;
    this.avatarUrl = avatarUrl;
    this.encryptedAccessToken = encryptedAccessToken;
    this.tokenType = tokenType != null ? tokenType : "bearer";
    this.scope = scope;
    this.connectedAt = Instant.now();
    this.updatedAt = Instant.now();
  }

  public Long getId() { return id; }
  public Long getUserId() { return userId; }
  public void setUserId(Long userId) { this.userId = userId; }
  public Long getGithubUserId() { return githubUserId; }
  public void setGithubUserId(Long githubUserId) { this.githubUserId = githubUserId; }
  public String getGithubUsername() { return githubUsername; }
  public void setGithubUsername(String githubUsername) { this.githubUsername = githubUsername; }
  public String getGithubEmail() { return githubEmail; }
  public void setGithubEmail(String githubEmail) { this.githubEmail = githubEmail; }
  public String getAvatarUrl() { return avatarUrl; }
  public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
  public String getEncryptedAccessToken() { return encryptedAccessToken; }
  public void setEncryptedAccessToken(String encryptedAccessToken) { this.encryptedAccessToken = encryptedAccessToken; }
  public String getTokenType() { return tokenType; }
  public void setTokenType(String tokenType) { this.tokenType = tokenType; }
  public String getScope() { return scope; }
  public void setScope(String scope) { this.scope = scope; }
  public Instant getConnectedAt() { return connectedAt; }
  public void setConnectedAt(Instant connectedAt) { this.connectedAt = connectedAt; }
  public Instant getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}