package com.policymesh.github;

import com.policymesh.auth.Role;
import com.policymesh.auth.User;
import com.policymesh.auth.UserRepository;
import com.policymesh.common.EncryptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class GitHubUserIsolationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private GitHubConnectionRepository connectionRepository;

  @Autowired
  private MonitoredRepositoryRepository monitoredRepository;

  @Autowired
  private EncryptionService encryptionService;

  private User userA;
  private User userB;

  @BeforeEach
  void setUp() {
    monitoredRepository.deleteAll();
    connectionRepository.deleteAll();
    userRepository.deleteAll();

    userA = userRepository.save(new User("usera@policymesh.io", "hashA", Role.COMPLIANCE_OFFICER));
    userB = userRepository.save(new User("userb@policymesh.io", "hashB", Role.COMPLIANCE_OFFICER));

    GitHubConnection connA = new GitHubConnection(
        userA.getId(), 1001L, "octocat-a", "usera@github.com",
        "https://avatar.a", encryptionService.encrypt("token-a"), "bearer", "read:user,repo"
    );
    connectionRepository.save(connA);

    GitHubConnection connB = new GitHubConnection(
        userB.getId(), 2002L, "octocat-b", "userb@github.com",
        "https://avatar.b", encryptionService.encrypt("token-b"), "bearer", "read:user,repo"
    );
    connectionRepository.save(connB);

    monitoredRepository.save(new MonitoredRepository(userA.getId(), 5001L, "org/repo-a", "repo-a", "org", "main", false));
    monitoredRepository.save(new MonitoredRepository(userB.getId(), 5002L, "org/repo-b", "repo-b", "org", "main", false));
  }

  @Test
  @WithMockUser(username = "usera@policymesh.io", roles = {"COMPLIANCE_OFFICER"})
  void userASeesOnlyTheirOwnGitHubAccount() throws Exception {
    mockMvc.perform(get("/api/v1/github/account"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.connected").value(true))
        .andExpect(jsonPath("$.username").value("octocat-a"))
        .andExpect(jsonPath("$.email").value("usera@github.com"));
  }

  @Test
  @WithMockUser(username = "userb@policymesh.io", roles = {"COMPLIANCE_OFFICER"})
  void userBSeesOnlyTheirOwnGitHubAccount() throws Exception {
    mockMvc.perform(get("/api/v1/github/account"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.connected").value(true))
        .andExpect(jsonPath("$.username").value("octocat-b"))
        .andExpect(jsonPath("$.email").value("userb@github.com"));
  }

  @Test
  void databaseLevelIsolationEnforcesUserBinding() {
    var reposA = monitoredRepository.findByUserIdOrderByRepoFullNameAsc(userA.getId());
    var reposB = monitoredRepository.findByUserIdOrderByRepoFullNameAsc(userB.getId());

    assertThat(reposA).hasSize(1);
    assertThat(reposA.get(0).getRepoFullName()).isEqualTo("org/repo-a");

    assertThat(reposB).hasSize(1);
    assertThat(reposB.get(0).getRepoFullName()).isEqualTo("org/repo-b");
  }
}