# PolicyMesh — Production Deployment Guide

This guide details the complete production deployment for PolicyMesh across **Vercel** (Frontend), **Render** (Backend & AI Service), and **Aiven** (Managed Cloud MySQL).

---

## 1. Target Production Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Vercel (React + Vite)                    │
│             https://policymesh.vercel.app                   │
└──────────────────────────────┬──────────────────────────────┘
                               │ HTTPS REST API
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                 Render (Spring Boot 3.3 API)                │
│            https://policymesh-backend.onrender.com          │
│                                                             │
│   ├── Policy Engine (DSL Compiler & Rule Evaluator)         │
│   ├── Runtime Enforcement Gate                              │
│   ├── CI Check Engine (GitHub REST API Integration)         │
│   └── Lineage Engine (SHA-256 Hash Chain Auditing)          │
└──────────────┬──────────────────────────────┬───────────────┘
               │                              │
               │ SSL (JDBC)                   │ HTTP Internal/External
               ▼                              ▼
┌──────────────────────────────┐ ┌─────────────────────────────┐
│      Aiven Cloud MySQL       │ │   Render AI Service         │
│  mysql-*.aivencloud.com      │ │   (Python / FastAPI)        │
└──────────────────────────────┘ └─────────────────────────────┘
```

---

## 2. Step 1: Provision Aiven MySQL Database

1. **Log in to Aiven Console** ([console.aiven.io](https://console.aiven.io)).
2. Click **Create Service** and select **MySQL**.
3. Choose your cloud provider and region (e.g. AWS or GCP, closest to your Render deployment region).
4. Select a plan (e.g. **Free Tier** or **Startup-4**).
5. Once the service status turns to **Running**, go to the **Overview** tab:
   - Note the **Host** (`mysql-xxxx.aivencloud.com`)
   - Note the **Port** (e.g. `18342`)
   - Note the **User** (`avnadmin`)
   - Note the **Password** (click to show/copy)
   - Note the **Database Name** (`defaultdb` or create `policymeshdb` under the *Databases* tab)
   - **SSL Mode**: Aiven requires SSL. PolicyMesh is pre-configured with `useSSL=true` and `sslMode=PREFERRED` / `REQUIRED`.

---

## 3. Step 2: Deploy Backend & AI Service to Render

### Option A: Deploy via Render Blueprint (Recommended)

1. Fork or push the PolicyMesh repository to your GitHub account (`VishalRajExe/PolicyMesh`).
2. Go to **Render Dashboard** ([dashboard.render.com](https://dashboard.render.com)).
3. Click **New +** $\rightarrow$ **Blueprint**.
4. Connect your GitHub repository.
5. Render will automatically parse [`render.yaml`](../render.yaml) and configure:
   - `policymesh-backend` (Docker Web Service)
   - `policymesh-ai-service` (Docker Web Service)
6. In the Blueprint parameters, supply your Aiven credentials:
   - `DB_HOST`: Your Aiven MySQL Host
   - `DB_PORT`: Your Aiven MySQL Port (e.g. `18342`)
   - `DB_NAME`: `defaultdb` (or `policymeshdb`)
   - `DB_USERNAME`: `avnadmin`
   - `DB_PASSWORD`: Your Aiven password
   - `GITHUB_TOKEN`: A GitHub Personal Access Token (with `repo` / `actions` read access)
   - `CORS_ALLOWED_ORIGINS`: `https://*.vercel.app,http://localhost:5173`
7. Click **Apply Blueprint** and wait for the builds to complete.

### Option B: Manual Web Service Setup on Render

#### 1. Backend Web Service (`policymesh-backend`):
- **Name**: `policymesh-backend`
- **Language / Environment**: `Docker`
- **Docker Context**: `backend`
- **Dockerfile Path**: `backend/Dockerfile`
- **Health Check Path**: `/health`
- **Environment Variables**:
  ```env
  DB_HOST=mysql-xxxx.aivencloud.com
  DB_PORT=18342
  DB_NAME=defaultdb
  DB_USERNAME=avnadmin
  DB_PASSWORD=your_aiven_password
  DB_USE_SSL=true
  DB_SSL_MODE=PREFERRED
  JWT_SECRET=your_32_plus_char_random_jwt_secret_key
  JWT_EXPIRATION_MS=86400000
  CORS_ALLOWED_ORIGINS=https://*.vercel.app,http://localhost:5173
  GITHUB_OWNER=VishalRajExe
  GITHUB_REPOSITORY=PolicyMesh
  GITHUB_TOKEN=ghp_your_github_token
  AI_SERVICE_MODE=remote
  AI_SERVICE_URL=https://policymesh-ai-service.onrender.com
  POLICYMESH_REDIS_ENABLED=false
  POLICYMESH_KAFKA_ENABLED=false
  POLICYMESH_DEMO_SEED=false
  POLICYMESH_DEMO_SEED_ENDPOINT=true
  ```

#### 2. AI Service Web Service (`policymesh-ai-service`):
- **Name**: `policymesh-ai-service`
- **Language / Environment**: `Docker`
- **Docker Context**: `ai-service`
- **Dockerfile Path**: `ai-service/Dockerfile`
- **Health Check Path**: `/health`
- **Environment Variables**:
  ```env
  APP_ENV=production
  AI_PROVIDER=mock
  AI_MODEL=gpt-4o-mini
  ALLOWED_ORIGINS=*
  ```

---

## 4. Step 3: Deploy React Frontend to Vercel

1. Log in to [Vercel](https://vercel.com).
2. Click **Add New...** $\rightarrow$ **Project**.
3. Import the `VishalRajExe/PolicyMesh` GitHub repository.
4. Configure Project Settings:
   - **Framework Preset**: `Vite`
   - **Root Directory**: `frontend` (or leave as `./` — root `vercel.json` and `frontend/vercel.json` both support building and routing)
   - **Build Command**: `npm run build`
   - **Output Directory**: `dist`
5. Configure Environment Variables:
   - `VITE_API_BASE_URL`: `https://policymesh-backend.onrender.com/api/v1` (Replace with your actual Render backend URL)
6. Click **Deploy**.
7. Once deployed, note your production Vercel URL (e.g. `https://policymesh.vercel.app`).

---

## 5. Step 4: Post-Deployment Verification Checklist

Execute these 6 verification tests against the live production environment:

### Test 1: Health & System Diagnostics
1. Open `https://<your-render-backend>.onrender.com/health` in browser $\rightarrow$ Expected: `{"status":"UP","service":"policymesh-backend",...}`.
2. Open `https://<your-render-backend>.onrender.com/actuator/health` $\rightarrow$ Expected: `{"status":"UP","components":{"db":{"status":"UP",...}}}`.

### Test 2: User Authentication & Role-Based Access Control
1. Open the Vercel app in your browser (`https://<your-vercel-domain>.vercel.app`).
2. Register an Admin user or log in with your credentials.
3. Verify JWT token is received and authenticated requests succeed.

### Test 3: Policy Management & YAML Import
1. Navigate to **/policies**.
2. Click **Import YAML** and click one of the template chips (*EU PII Gate*).
3. Click **Import Policy** $\rightarrow$ Verify policy is compiled and persisted in Aiven MySQL.

### Test 4: Service Graph & Data Flow Enforcement
1. Navigate to **/services** and add test services (`orders-api` [EU], `analytics-api` [US]).
2. Navigate to **/runtime** $\rightarrow$ Test cross-border enforcement:
   - `orders-api` [EU] $\rightarrow$ `payments-api` [EU] (`PII`) $\rightarrow$ **ALLOW**
   - `orders-api` [EU] $\rightarrow$ `analytics-api` [US] (`PII`) $\rightarrow$ **DENY** (Violates EU-PII-001)

### Test 5: Cryptographic Lineage Audit Trail
1. Navigate to **/lineage**.
2. Verify each decision is chained with a SHA-256 cryptographic hash and tamper-evident signatures.

### Test 6: GitHub Actions & CI Commit Gate Verification
1. Navigate to **/ci-check**.
2. Enter branch `main` and a real commit SHA from GitHub.
3. Click **Run Compliance Gate**.
4. Verify backend queries the GitHub REST API, displays real check run statuses (Passed / Failed / Skipped), correlates with Policy compliance, and computes the correct **Final Merge Decision**.