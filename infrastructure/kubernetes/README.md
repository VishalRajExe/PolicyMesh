# Future Kubernetes deployment

These are templates, not a claim of production readiness. Build and publish immutable backend/AI images, replace the `REPLACE_WITH_*_IMAGE` placeholders, provide secrets through an approved secret manager, and validate probes against the actual release before applying.

```bash
kubectl apply -f namespace.yaml
kubectl apply -f configmap.yaml
# Create the real Secret out-of-band; never apply secrets.example.yaml unchanged.
kubectl apply -f backend/
kubectl apply -f ai-service/
```

Deploy backend and AI service as deployments behind cluster services. PostgreSQL, Redis, and Kafka should normally be managed services, or use vetted operators/StatefulSets with backups, storage classes, TLS, authentication, topology spreading, monitoring, and tested recovery. They intentionally have documentation only in this starter layout.
