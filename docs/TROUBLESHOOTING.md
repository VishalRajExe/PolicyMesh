# TROUBLESHOOTING.md

See [DOCKER_SETUP.md](./DOCKER_SETUP.md) and [LOCAL_DEVELOPMENT.md](./LOCAL_DEVELOPMENT.md) for baseline setup.

## PostgreSQL Won't Connect

- Check the container is running: `docker compose ps`
- Verify `SPRING_DATASOURCE_URL` matches the Compose service name/port (`localhost:5432` from the host, `postgres:5432` from another container).
- Check logs: `docker compose logs postgres`

## Redis Unavailable

- PolicyMesh should continue operating (see [REDIS.md](./REDIS.md) §Failure Behavior) — this is a degraded-performance situation, not a hard failure.
- Restart: `docker compose restart redis`

## Kafka Unavailable

- Event publishing should log a warning and continue (see [KAFKA.md](./KAFKA.md) §Fallback Behavior) — no request should fail because of this.
- Restart: `docker compose restart kafka`

## JWT Invalid

- Confirm `JWT_SECRET` is identical between the token-issuing process and the validating process (a restart with a new random secret invalidates all existing tokens).
- Check token expiration — default is 1 hour (see [AUTHENTICATION.md](./AUTHENTICATION.md)).

## Policy Validation Fails

- Re-check the DSL rules in [POLICY_DSL.md](./POLICY_DSL.md) §Validation Rules — most commonly, `allowedRegions` and `deniedRegions` overlap, or `allowedRegions` is empty.

## No Matching Policy

- Expected behavior, not a bug: an unmatched data class/jurisdiction pair is DENY by default (see [POLICY_DSL.md](./POLICY_DSL.md) §Deterministic Behavior). Create the missing policy.

## CI Check Fails Unexpectedly

- Run `GET /graph` and inspect the current registered edges — a stale or incorrectly-registered service/edge is the most common cause, not a bug in the checker itself.
- Reproduce locally with `POST /ci/check` before assuming CI infra is at fault (see [CI_INTEGRATION.md](./CI_INTEGRATION.md) §Local CI Testing).

## Lineage Verification Fails

- `GET /lineage/verify` returning `valid: false` means the hash chain is genuinely broken — this should never happen from normal application use; check whether any direct database edits were made against `LineageRecord` or `Decision` rows.

## Docker Container Won't Start

- `docker compose logs <service>` to view the failure reason.
- Confirm no other process is using the same port (see next item).

## Port Already in Use

```bash
lsof -i :8080   # find what's using the port
kill -9 <pid>   # or change the port mapping in docker-compose.yml / application.yml
```

## Maven Build Fails

- Confirm Java 21 is active: `java -version`
- Clear stale artifacts: `mvn clean install`
- Check for a dependency resolution issue: `mvn dependency:tree`
