package com.policymesh.health;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class HealthEndpointTest {

  @Autowired
  private MockMvc mvc;

  @Test
  @DisplayName("GET /health without token returns HTTP 200 OK with status: ok")
  void testPublicHealthEndpoint() throws Exception {
    mvc.perform(get("/health"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status", is("ok")))
        // Ensure no internal / sensitive fields are leaked
        .andExpect(jsonPath("$.database").doesNotExist())
        .andExpect(jsonPath("$.redis").doesNotExist())
        .andExpect(jsonPath("$.kafka").doesNotExist())
        .andExpect(jsonPath("$.secret").doesNotExist())
        .andExpect(jsonPath("$.token").doesNotExist());
  }

  @Test
  @DisplayName("GET / (root) without token returns HTTP 200 OK with status: ok")
  void testRootHealthEndpoint() throws Exception {
    mvc.perform(get("/"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status", is("ok")));
  }

  @Test
  @DisplayName("Protected business endpoints require authentication")
  void testProtectedEndpointsRemainSecured() throws Exception {
    // Unauthenticated GET /api/v1/policies should fail with 401 Unauthorized
    mvc.perform(get("/api/v1/policies"))
        .andExpect(status().isUnauthorized());

    // Unauthenticated POST /api/v1/ci/check should fail with 401 Unauthorized
    mvc.perform(post("/api/v1/ci/check")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"branch\":\"main\",\"commitHash\":\"abc\"}"))
        .andExpect(status().isUnauthorized());
  }
}
