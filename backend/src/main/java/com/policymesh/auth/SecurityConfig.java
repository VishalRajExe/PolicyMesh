package com.policymesh.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * RBAC matrix from docs/AUTHENTICATION.md (authoritative):
 * - policies write: ADMIN, COMPLIANCE_OFFICER; delete: ADMIN only
 * - services/edges write: ADMIN, ENGINEER; delete: ADMIN only
 * - enforce check: ADMIN, ENGINEER
 * - CI check: ADMIN, COMPLIANCE_OFFICER, ENGINEER
 * - AI classify: ADMIN, COMPLIANCE_OFFICER, ENGINEER; approve/reject: ADMIN, COMPLIANCE_OFFICER
 * - graph validate, lineage, dashboard, all GETs: any authenticated role (VIEWER included)
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

  @Bean
  CorsConfigurationSource corsConfigurationSource(@Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:5173,https://*.vercel.app}") String origins) {
    CorsConfiguration config = new CorsConfiguration();
    List<String> originList = Arrays.stream(origins.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    config.setAllowedOriginPatterns(originList);
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With"));
    config.setExposedHeaders(List.of("Location"));
    // Never allow credentials with bare wildcard origin "*"
    config.setAllowCredentials(!originList.contains("*"));
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }

  @Bean
  SecurityFilterChain security(HttpSecurity http, JwtAuthenticationFilter filter) throws Exception {
    return http
        .csrf(c -> c.disable())
        .cors(c -> {}) // picks up the corsConfigurationSource bean by name
        .headers(h -> h
            .contentTypeOptions(org.springframework.security.config.Customizer.withDefaults())
            .frameOptions(f -> f.deny())
            .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'none'; frame-ancestors 'none'")))
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .exceptionHandling(e -> e
            .authenticationEntryPoint((req, res, ex) -> problem(res, HttpStatus.UNAUTHORIZED, "Authentication required: provide a valid bearer token", req))
            .accessDeniedHandler((req, res, ex) -> problem(res, HttpStatus.FORBIDDEN, "Access denied: your role is not permitted to perform this action", req)))
        .authorizeHttpRequests(a -> a
            .requestMatchers("/api/v1/auth/**", "/health", "/actuator/**").permitAll()
            .requestMatchers("/api/v1/users/roles").authenticated()
            .requestMatchers("GET", "/api/v1/users/**").hasAnyRole("ADMIN", "COMPLIANCE_OFFICER")
            .requestMatchers("POST", "/api/v1/users/**").hasRole("ADMIN")
            .requestMatchers("PUT", "/api/v1/users/**").hasRole("ADMIN")
            .requestMatchers("DELETE", "/api/v1/users/**").hasRole("ADMIN")
            .requestMatchers("/api/v1/reports/**").authenticated()
            .requestMatchers("/api/v1/settings/**").authenticated()
            .requestMatchers("POST", "/api/v1/policies/**").hasAnyRole("ADMIN", "COMPLIANCE_OFFICER")
            .requestMatchers("PUT", "/api/v1/policies/**").hasAnyRole("ADMIN", "COMPLIANCE_OFFICER")
            .requestMatchers("DELETE", "/api/v1/policies/**").hasRole("ADMIN")
            .requestMatchers("POST", "/api/v1/services").hasAnyRole("ADMIN", "ENGINEER")
            .requestMatchers("PUT", "/api/v1/services/**").hasAnyRole("ADMIN", "ENGINEER")
            .requestMatchers("DELETE", "/api/v1/services/**").hasRole("ADMIN")
            .requestMatchers("POST", "/api/v1/edges").hasAnyRole("ADMIN", "ENGINEER")
            .requestMatchers("PUT", "/api/v1/edges/**").hasAnyRole("ADMIN", "ENGINEER")
            .requestMatchers("DELETE", "/api/v1/edges/**").hasAnyRole("ADMIN", "ENGINEER")
            .requestMatchers("POST", "/api/v1/enforce/check").hasAnyRole("ADMIN", "ENGINEER")
            .requestMatchers("POST", "/api/v1/ci/check").hasAnyRole("ADMIN", "COMPLIANCE_OFFICER", "ENGINEER")
            .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/ai/**").authenticated()
            .requestMatchers("POST", "/api/v1/ai/classify", "/api/v1/ai/classifications").hasAnyRole("ADMIN", "COMPLIANCE_OFFICER", "ENGINEER")
            .requestMatchers("POST", "/api/v1/ai/classify/*/approve", "/api/v1/ai/classify/*/reject",
                             "/api/v1/ai/classifications/*/approve", "/api/v1/ai/classifications/*/reject").hasAnyRole("ADMIN", "COMPLIANCE_OFFICER")
            .requestMatchers("POST", "/api/v1/compiler/compile").hasAnyRole("ADMIN", "COMPLIANCE_OFFICER", "ENGINEER")
            .requestMatchers("POST", "/api/v1/dev/seed").hasRole("ADMIN")
            .anyRequest().authenticated())
        .addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class)
        .build();
  }

  private static void problem(jakarta.servlet.http.HttpServletResponse res, HttpStatus status, String detail,
                              jakarta.servlet.http.HttpServletRequest req) throws java.io.IOException {
    res.setStatus(status.value());
    res.setContentType("application/problem+json");
    res.getWriter().write("""
        {"type":"https://policymesh/errors/%s","title":"%s","status":%d,"detail":"%s","instance":"%s"}"""
        .formatted(status == HttpStatus.UNAUTHORIZED ? "unauthorized" : "forbidden",
            status.getReasonPhrase(), status.value(), detail, req.getRequestURI()));
  }
}
