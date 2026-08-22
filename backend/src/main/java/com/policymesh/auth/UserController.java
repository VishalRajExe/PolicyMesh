package com.policymesh.auth;

import com.policymesh.common.ApiException;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

  private final UserRepository users;
  private final PasswordEncoder encoder;

  public UserController(UserRepository users, PasswordEncoder encoder) {
    this.users = users;
    this.encoder = encoder;
  }

  @GetMapping
  public List<UserDtos.Response> list() {
    return users.findAll().stream().map(UserDtos.Response::from).toList();
  }

  @GetMapping("/{id}")
  public UserDtos.Response get(@PathVariable Long id) {
    return UserDtos.Response.from(findUser(id));
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public UserDtos.Response create(@Valid @RequestBody UserDtos.CreateRequest req) {
    String email = req.email().trim().toLowerCase();
    if (users.findByEmailIgnoreCase(email).isPresent()) {
      throw ApiException.conflict("A user with email '" + email + "' already exists");
    }
    User user = new User();
    user.setEmail(email);
    user.setPasswordHash(encoder.encode(req.password()));
    user.setRole(req.role() != null ? req.role() : Role.ENGINEER);
    user.setStatus(req.status() != null && !req.status().isBlank() ? req.status().trim().toUpperCase() : "ACTIVE");
    return UserDtos.Response.from(users.save(user));
  }

  @PutMapping("/{id}")
  public UserDtos.Response update(@PathVariable Long id, @Valid @RequestBody UserDtos.UpdateRequest req, Principal principal) {
    User user = findUser(id);
    if (req.role() != null) {
      if (principal != null && user.getEmail().equalsIgnoreCase(principal.getName()) && req.role() != user.getRole()) {
        throw ApiException.badRequest("You cannot change your own role");
      }
      user.setRole(req.role());
    }
    if (req.status() != null && !req.status().isBlank()) {
      String nextStatus = req.status().trim().toUpperCase();
      if (principal != null && user.getEmail().equalsIgnoreCase(principal.getName()) && !"ACTIVE".equals(nextStatus)) {
        throw ApiException.badRequest("You cannot deactivate your own account");
      }
      user.setStatus(nextStatus);
    }
    return UserDtos.Response.from(users.save(user));
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id, Principal principal) {
    User user = findUser(id);
    if (principal != null && user.getEmail().equalsIgnoreCase(principal.getName())) {
      throw ApiException.badRequest("You cannot delete your own account");
    }
    users.delete(user);
  }

  @GetMapping("/roles")
  public List<UserDtos.RolePermission> roles() {
    return List.of(
        new UserDtos.RolePermission(
            "ADMIN",
            "Administrator",
            "Full control over policies, services, data flow graph, runtime enforcement, users, and settings.",
            List.of(
                "Manage Policies (Create, Update, Delete)",
                "Manage Services & Edges (Create, Update, Delete)",
                "Run Runtime Enforcement Checks",
                "Execute CI Compliance Checks",
                "AI Schema Classification (Classify, Approve, Reject)",
                "View Lineage Ledger & Verify Chains",
                "Access Compliance Reports & CSV Exports",
                "Manage Users & Roles (Full CRUD)",
                "Configure Platform Settings"
            )
        ),
        new UserDtos.RolePermission(
            "COMPLIANCE_OFFICER",
            "Compliance Officer",
            "Governance and compliance auditor. Manages policies, approves AI classifications, and reviews audit reports.",
            List.of(
                "Manage Policies (Create, Update)",
                "Review & Execute CI Compliance Scans",
                "AI Schema Classification (Classify, Approve, Reject)",
                "View Lineage Ledger & Verify Cryptographic Integrity",
                "Access Compliance Reports & CSV Audit Exports",
                "View User Directory & Roles"
            )
        ),
        new UserDtos.RolePermission(
            "ENGINEER",
            "Integration Engineer",
            "Builds and maintains service topologies, tests enforcement, and runs pre-merge CI checks.",
            List.of(
                "Register Services & Data Flow Edges",
                "Run Runtime Enforcement Checks",
                "Run CI Compliance Checks",
                "Request AI Sensitivity Classification",
                "View Lineage History & Audit Graph",
                "View Compliance Reports"
            )
        ),
        new UserDtos.RolePermission(
            "VIEWER",
            "Read-Only Viewer",
            "Auditor or observer. Read-only access across dashboard, service graph, and verified lineage.",
            List.of(
                "View Dashboard Metrics",
                "View Policy Definitions",
                "View Service Graph Topologies",
                "View Verified Lineage Chains",
                "View Compliance Reports"
            )
        )
    );
  }

  private User findUser(Long id) {
    return users.findById(id).orElseThrow(() -> ApiException.notFound("User not found with ID: " + id));
  }
}
