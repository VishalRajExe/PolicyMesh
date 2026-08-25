package com.policymesh;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.policymesh.auth.User;
import com.policymesh.auth.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:reg_integration_test;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "management.health.redis.enabled=false",
    "management.health.kafka.enabled=false",
    "policymesh.kafka.enabled=false",
    "policymesh.redis.enabled=false"
})
@AutoConfigureMockMvc
class UserRegistrationIntegrationTest {

  @Autowired private MockMvc mvc;
  @Autowired private ObjectMapper mapper;
  @Autowired private UserRepository userRepository;

  @Test
  @DisplayName("POST /api/v1/auth/register creates user with enabled=true and safe defaults")
  void testRegistrationWithEnabledDefault() throws Exception {
    String payload = """
        {
          "name": "Sarah Connor",
          "email": "sarah.connor@cyberdyne.io",
          "password": "Password123!",
          "role": "COMPLIANCE_OFFICER"
        }
        """;

    mvc.perform(post("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.email").value("sarah.connor@cyberdyne.io"))
        .andExpect(jsonPath("$.role").value("COMPLIANCE_OFFICER"));

    // Verify entity in repository has enabled=true, status=ACTIVE, and name set
    User saved = userRepository.findByEmailIgnoreCase("sarah.connor@cyberdyne.io").orElseThrow();
    assertThat(saved.isEnabled()).isTrue();
    assertThat(saved.getStatus()).isEqualTo("ACTIVE");
    assertThat(saved.getName()).isEqualTo("Sarah Connor");
    assertThat(saved.getCreatedAt()).isNotNull();
    assertThat(saved.getUpdatedAt()).isNotNull();
  }

  @Test
  @DisplayName("Duplicate email registration returns HTTP 409 Conflict")
  void testDuplicateRegistrationConflict() throws Exception {
    String payload = """
        {
          "name": "Kyle Reese",
          "email": "kyle.reese@resistance.io",
          "password": "Password123!",
          "role": "ENGINEER"
        }
        """;

    mvc.perform(post("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload))
        .andExpect(status().isCreated());

    mvc.perform(post("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload))
        .andExpect(status().isConflict());
  }

  @Test
  @DisplayName("GET /api/v1/auth/register is rejected with Method Not Allowed (405)")
  void testGetRegisterNotAllowed() throws Exception {
    mvc.perform(get("/api/v1/auth/register"))
        .andExpect(status().isMethodNotAllowed());
  }

  @Test
  @DisplayName("POST /api/v1/auth/login succeeds after registration")
  void testLoginAfterRegistration() throws Exception {
    String email = "john.connor@resistance.io";
    String registerPayload = String.format("""
        {
          "name": "John Connor",
          "email": "%s",
          "password": "Password123!",
          "role": "ADMIN"
        }
        """, email);

    mvc.perform(post("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(registerPayload))
        .andExpect(status().isCreated());

    String loginPayload = String.format("""
        {
          "email": "%s",
          "password": "Password123!"
        }
        """, email);

    mvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(loginPayload))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").isString())
        .andExpect(jsonPath("$.role").value("ADMIN"));
  }
}
