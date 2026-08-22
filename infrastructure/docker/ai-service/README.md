# AI service container

The Compose stack builds the existing `../../ai-service/Dockerfile` and checks `GET /health`. It uses mock mode by default and reaches a future backend at `http://backend:8080`. AI failure must not disable core policy enforcement.
