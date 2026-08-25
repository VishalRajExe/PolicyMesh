package com.policymesh.health;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Public, ultra-lightweight liveness health check endpoint.
 *
 * <p>Requirements:
 * <ul>
 *   <li>Returns HTTP 200 OK with {"status": "ok"}.</li>
 *   <li>Public access without authentication or JWT tokens.</li>
 *   <li>Zero downstream queries (no MySQL, Redis, Kafka, GitHub, or AI service calls).</li>
 *   <li>Designed for keep-alive monitoring and liveness probes (e.g. Render, UptimeRobot).</li>
 * </ul>
 */
@RestController
public class HealthController {

  private static final Map<String, String> OK_RESPONSE = Map.of("status", "ok");

  @GetMapping(value = {"/health", "/"}, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Map<String, String>> health() {
    return ResponseEntity.ok(OK_RESPONSE);
  }
}