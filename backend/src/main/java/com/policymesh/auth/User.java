package com.policymesh.auth;
import jakarta.persistence.*; import java.time.Instant;
@Entity @Table(name="users", uniqueConstraints=@UniqueConstraint(columnNames="email"))
public class User { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @Column(nullable=false) private String email; @Column(nullable=false) private String passwordHash; @Enumerated(EnumType.STRING) @Column(nullable=false) private Role role; @Column(nullable=false,updatable=false) private Instant createdAt=Instant.now();
 public Long getId(){return id;} public String getEmail(){return email;} public String getPasswordHash(){return passwordHash;} public Role getRole(){return role;} public void setEmail(String v){email=v;} public void setPasswordHash(String v){passwordHash=v;} public void setRole(Role v){role=v;} }
