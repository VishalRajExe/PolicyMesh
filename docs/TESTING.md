# TESTING.md

See [GRAPH_ENGINE.md](./GRAPH_ENGINE.md), [LINEAGE_LEDGER.md](./LINEAGE_LEDGER.md), and [CI_INTEGRATION.md](./CI_INTEGRATION.md) for the behaviors under test.

## Strategy

| Layer | Tool/Approach |
|---|---|
| Unit tests | JUnit 5 + Mockito for Policy Engine, Graph Engine, Lineage hashing logic |
| Integration tests | `@SpringBootTest` with Testcontainers PostgreSQL |
| Repository tests | `@DataJpaTest` against a real (containerized) PostgreSQL |
| Controller tests | `@WebMvcTest` / MockMvc for request validation and status codes |
| Security tests | Verify role matrix from [AUTHENTICATION.md](./AUTHENTICATION.md) — each endpoint tested against each role |
| Policy engine tests | Table-driven tests covering the deterministic-behavior matrix in [POLICY_DSL.md](./POLICY_DSL.md) |
| Graph tests | Valid/invalid graphs, cycles, duplicate edges (see [GRAPH_ENGINE.md](./GRAPH_ENGINE.md)) |
| Lineage tests | Hash-chain creation and verification, tamper detection |
| CI tests | `POST /ci/check` PASS/FAIL against known-good and known-bad graphs |

## Mandatory Cases

### Policy Engine

```text
EU PII    -> EU  = ALLOW
EU PII    -> US  = DENY
EU PII    -> CN  = DENY
EU PUBLIC -> US  = ALLOW   (non-restricted data class)
```

### Graph

```text
Valid graph (no disallowed edges)     = PASS
Graph containing one invalid edge     = FAIL, violation lists that edge
```

### Lineage

```text
Untouched chain                        = valid
Modified record content                = invalid, brokenAt = that record
Broken previousHash link               = invalid, brokenAt = that record
```

## Running Tests

```bash
mvn test
```

## Coverage Expectations

The Policy Engine, Graph Engine, and Lineage Ledger are the compliance-critical paths and should have the highest test coverage; UI-adjacent code (DTO mapping, controllers) needs correctness tests but not exhaustive coverage for the hackathon MVP.
