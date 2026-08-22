package com.policymesh.config;

import com.policymesh.auth.security.JwtAuthenticationFilter;
import com.policymesh.common.constants.Roles;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Stateless JWT-based security. RBAC enforced with role checks on
 * write endpoints; read endpoints are open to any authenticated role.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserDetailsService userDetailsService;
    private final CorsConfigurationSource corsConfigurationSource;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/policies").hasAnyRole(Roles.ADMIN, Roles.COMPLIANCE_OFFICER)
                        .requestMatchers(HttpMethod.PUT, "/api/v1/policies/**").hasAnyRole(Roles.ADMIN, Roles.COMPLIANCE_OFFICER)
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/policies/**").hasAnyRole(Roles.ADMIN, Roles.COMPLIANCE_OFFICER)
                        .requestMatchers(HttpMethod.POST, "/api/v1/services").hasAnyRole(Roles.ADMIN, Roles.ENGINEER)
                        .requestMatchers(HttpMethod.PUT, "/api/v1/services/**").hasAnyRole(Roles.ADMIN, Roles.ENGINEER)
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/services/**").hasAnyRole(Roles.ADMIN, Roles.ENGINEER)
                        .requestMatchers("/api/v1/ci/**").hasAnyRole(Roles.ADMIN, Roles.ENGINEER, Roles.COMPLIANCE_OFFICER)
                        .requestMatchers("/api/v1/enforce/**").hasAnyRole(Roles.ADMIN, Roles.ENGINEER, Roles.COMPLIANCE_OFFICER)
                        .requestMatchers("/api/v1/ai/**").hasAnyRole(Roles.ADMIN, Roles.COMPLIANCE_OFFICER)
                        .requestMatchers(HttpMethod.GET, "/api/v1/**").authenticated()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
