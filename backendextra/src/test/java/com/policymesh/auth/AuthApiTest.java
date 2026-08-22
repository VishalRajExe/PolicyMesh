package com.policymesh.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void register_thenLogin_returnsJwtToken() throws Exception {
        Map<String, Object> registerBody = Map.of(
                "name", "Test Officer",
                "email", "officer@policymesh.io",
                "password", "SecurePass123",
                "role", "COMPLIANCE_OFFICER"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerBody)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("officer@policymesh.io"))
                .andExpect(jsonPath("$.role").value("COMPLIANCE_OFFICER"));

        Map<String, Object> loginBody = Map.of("email", "officer@policymesh.io", "password", "SecurePass123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value("COMPLIANCE_OFFICER"));
    }

    @Test
    void duplicateRegistration_returnsConflict() throws Exception {
        Map<String, Object> body = Map.of(
                "name", "Dup User", "email", "dup@policymesh.io",
                "password", "SecurePass123", "role", "VIEWER");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("https://policymesh/errors/duplicate"));
    }

    @Test
    void unauthenticatedAccessToProtectedEndpoint_isRejected() throws Exception {
        mockMvc.perform(get("/api/v1/policies"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void invalidLogin_returnsUnauthorizedProblemDetail() throws Exception {
        Map<String, Object> body = Map.of("email", "nobody@policymesh.io", "password", "wrongpassword");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized());
    }
}
