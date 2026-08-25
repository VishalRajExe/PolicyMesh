# PolicyMesh — Automated GitHub Webhook Integration Guide

This guide describes how to configure the automated GitHub Webhook integration for PolicyMesh. When developers `git push` commits to the repository, GitHub automatically delivers a signed event to the PolicyMesh backend, which analyzes changed files against active data residency policies and records the compliance status in real-time.

---

## 1. Automated Workflow

```
 Developer
    │  git commit
    │  git push
    ▼
 GitHub Repository
    │  POST /api/webhooks/github
    ▼
 PolicyMesh Backend (Render / Cloud)
    │
    ├── 1. Verify X-Hub-Signature-256 (HMAC-SHA256 with GITHUB_WEBHOOK_SECRET)
    ├── 2. Check X-GitHub-Delivery (Replay protection & idempotency)
    ├── 3. Respond HTTP 202 Accepted (Non-blocking)
    │
    ▼ (Asynchronous Background Engine)
 ┌───────────────────────────────────────────────────────────┐
 │ Fetch Commit SHA & Changed Files via GitHub REST API      │
 │ Parse Service Graph & Evaluate Active DSL Policies        │
 │ Correlate with GitHub Actions Workflow Check Runs         │
 │ Persist Scan Report in MySQL & Lineage Chain              │
 └───────────────────────────────────────────────────────────┘
    │
    ▼
 PolicyMesh Dashboard & CI Compliance Gate (/ci-check)
```

---

## 2. GitHub OAuth App Setup ("Connect GitHub")

1. In GitHub, go to **Settings** $\rightarrow$ **Developer settings** $\rightarrow$ **OAuth Apps** $\rightarrow$ **New OAuth App**.
2. Fill in:
   - **Application name**: `PolicyMesh Compliance Guard`
   - **Homepage URL**: `https://policymesh.vercel.app` (or `http://localhost:5173` for local dev)
   - **Authorization callback URL**: `https://<your-render-backend-url>.onrender.com/api/v1/github/callback` (or `http://localhost:8080/api/v1/github/callback` locally)
3. Click **Register application**.
4. Generate a **Client secret**.
5. Set `GITHUB_CLIENT_ID` and `GITHUB_CLIENT_SECRET` in your backend environment variables (`render.yaml` / `.env`).

---

## 3. GitHub Webhook Configuration

1. In your GitHub repository ([`VishalRajExe/PolicyMesh`](https://github.com/VishalRajExe/PolicyMesh)), navigate to **Settings** $\rightarrow$ **Webhooks** $\rightarrow$ **Add webhook**.
2. Configure the following fields:
   - **Payload URL**: `https://<your-render-backend-url>.onrender.com/api/webhooks/github`
   - **Content type**: `application/json`
   - **Secret**: A strong random string (e.g. 32+ characters). Copy this value into your server environment variable `GITHUB_WEBHOOK_SECRET`.
   - **SSL verification**: Select **Enable SSL verification**.
   - **Which events would you like to trigger this webhook?**: Select **Just the push event** (and Pings).
3. Click **Add webhook**. GitHub will immediately send a `ping` event. PolicyMesh will verify the HMAC signature and respond with `HTTP 200 PONG`.

---

## 3. GitHub Permissions & Least Privilege Principle

PolicyMesh adheres strictly to the principle of least privilege:
* **Permissions Required**:
  - `Repository metadata`: **Read-only**
  - `Repository contents`: **Read-only**
  - `Commit statuses / Check runs`: **Read-only**
* **Permissions NEVER Requested**:
  - ❌ No repository write or push access
  - ❌ No repository administration or deletion access
  - ❌ No workflow file modification access
  - ❌ No organization administration access
  - ❌ No arbitrary code or script execution

---

## 4. Security Protections Implemented

| Security Mechanism | Implementation |
| :--- | :--- |
| **Cryptographic Authentication** | Computes `HmacSHA256(payloadBytes, GITHUB_WEBHOOK_SECRET)` and uses `MessageDigest.isEqual` for constant-time comparison to prevent timing attacks. Rejects invalid or missing signatures with `HTTP 401`. |
| **Replay Attack Protection** | Records `X-GitHub-Delivery` UUIDs in `webhook_deliveries` table. Duplicate delivery attempts return `HTTP 200 ALREADY_PROCESSED` without re-running analysis. |
| **Non-Blocking Execution** | Returns `HTTP 202 Accepted` immediately so GitHub webhook deliveries never time out. Analysis runs asynchronously via Spring `@Async`. |
| **Untrusted Input Sanitization** | Strict regex validation on commit SHAs (`^[0-9a-fA-F]{40}$`), max payload limit (5MB), and safe JSON tree parsing without code evaluation. |
| **Server-Side Secret Isolation** | `GITHUB_WEBHOOK_SECRET` and `GITHUB_TOKEN` remain strictly on the backend and are never returned in API responses or sent to the React frontend. |

---

## 5. Local Testing Instructions

You can simulate GitHub webhook deliveries locally using the provided script:

```powershell
# In PowerShell:
.\scripts\test-github-webhook.ps1 -Secret "your_webhook_secret" -CommitSha "40905bd" -Branch "main"
```

Or using Bash/cURL:
```bash
./scripts/test-github-webhook.sh http://127.0.0.1:8080 "your_webhook_secret" main 40905bd
```