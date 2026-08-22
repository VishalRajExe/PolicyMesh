# PolicyMesh — Design & Integration Guide

How the React frontend and Spring Boot backend fit together, how to run
them side by side, and how the dashboard in `policymesh-frontend/` maps
onto real backend data.

```
┌─────────────────────────┐        HTTPS / JSON        ┌──────────────────────────┐
│  policymesh-frontend     │  ────────────────────────▶ │  policymesh-backend      │
│  React + Vite (5173)      │  ◀────────────────────────  │  Spring Boot (8080)      │
│  src/api/*.js  (Axios)    │        Bearer <JWT>         │  /api/v1/**              │
└─────────────────────────┘                              └──────────────────────────┘
                                                                     │
                                                     ┌───────────────┼───────────────┐
                                                     ▼               ▼               ▼
                                                PostgreSQL         Redis           Kafka
                                               (source of truth) (cache only)  (async events)
```

---

## 1. Repository layout

Put both projects side by side (or as two folders in one monorepo):

```
policymesh/
  backend/      Spring Boot API (see backend/README.md)
  frontend/     React dashboard (this project)
  design.md     <- this file
```

---

## 2. Running everything together

### Step 1 — start infrastructure + backend

```bash
cd backend
docker compose up -d postgres redis kafka zookeeper
mvn spring-boot:run
```

Confirm it's up:

```bash
curl http://localhost:8080/actuator/health
```

### Step 2 — start the frontend

```bash
cd frontend
npm install
cp .env.example .env
# .env → VITE_API_BASE_URL=http://localhost:8080/api/v1
npm run dev
```

Open `http://localhost:5173`. Register a user, log in, and the dashboard
starts pulling real data from the backend.

### Step 3 — seed demo data (optional but recommended)

```bash
cd backend
./scripts/seed-demo-data
```

This gives you the `EU-PII-001` / `IN-PII-001` policies and the
`orders-api → analytics-api` violation used throughout the backend's
acceptance tests, so the frontend has something real to show immediately.

---

## 3. How the frontend talks to the backend

All backend calls go through `frontend/src/api/client.js`, a single Axios
instance:

```js
export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL, // e.g. http://localhost:8080/api/v1
});
```

Two interceptors do all the cross-cutting work:

- **Request interceptor** — reads the JWT from `localStorage` and attaches
  `Authorization: Bearer <token>` to every request.
- **Response interceptor** — on `401`, clears the stored session and
  redirects to `/login`; on any other error, unwraps the backend's RFC 7807
  `application/problem+json` body (`{ type, title, detail, ... }`) into a
  plain `Error(detail)` so components can just do `catch (err) { setError(err.message) }`.

Every backend module has a matching frontend file, so there's a 1:1 map
between REST resource and JS module:

| Backend module | Endpoints | Frontend file |
|---|---|---|
| `auth` | `/auth/register`, `/auth/login` | `src/api/auth.js` |
| `policy` | `/policies*` | `src/api/policies.js` |
| `servicegraph` | `/services*`, `/edges*` | `src/api/services.js` |
| `graph` | `/graph`, `/graph/validate` | `src/api/graph.js` |
| `enforcement` | `/enforce/check` | `src/api/enforcement.js` |
| `ci` | `/ci/check`, `/ci/scans/{id}` | `src/api/ci.js` |
| `lineage` | `/lineage*` | `src/api/lineage.js` |
| `dashboard` | `/dashboard/summary` | `src/api/dashboard.js` |
| `audit` | `/audit/recent` | `src/api/audit.js` |
| `ai` | `/ai/classify*` | `src/api/ai.js` |

Import what you need from the barrel file:

```js
import { policiesApi, enforcementApi } from "../api";

const policies = await policiesApi.list();
const result = await enforcementApi.check({
  sourceService: "orders-api",
  destinationService: "analytics-api",
  sourceRegion: "EU",
  destinationRegion: "US",
  dataClassTags: ["PII"],
});
```

---

## 4. Auth flow

1. `POST /api/v1/auth/register` — creates a user with a role
   (`ADMIN` / `COMPLIANCE_OFFICER` / `ENGINEER` / `VIEWER`).
2. `POST /api/v1/auth/login` — returns `{ token, role, email, expiresInMs }`.
3. The frontend's `AuthContext` (`src/context/AuthContext.jsx`) stores the
   token in `localStorage` under `policymesh_token` and the `{ email, role }`
   pair under `policymesh_user`.
4. `ProtectedRoute` (`src/components/layout/ProtectedRoute.jsx`) checks for
   a token before rendering any authenticated page, and wraps the page in
   `AppShell` (sidebar + status bar).
5. On any `401` response, the Axios interceptor clears storage and sends
   the user back to `/login` automatically — no per-page logic needed.

Role-gating on the frontend is cosmetic only (hide a button); the backend's
`SecurityConfig` is the actual enforcement boundary. Don't rely on hiding a
button in React as your only access control.

---

## 5. Dashboard data mapping

`src/pages/Dashboard.jsx` is the page shown in the reference design. Here's
where each panel's data comes from and what to change to make it fully
live:

| Panel | Current source | Backend endpoint to wire in |
|---|---|---|
| Stat cards (Total Policies, Active Policies, Data Flows Checked, Blocked Flows, Compliance Score) | `useDashboardData()` → `dashboardApi.summary()` | `GET /dashboard/summary` (already wired) |
| Policy Status Overview (donut) | static `policyDonutData` | Add a `status` breakdown to `GET /dashboard/summary`, or fetch `GET /policies` and group by `status` client-side |
| Data Flow Decisions (line chart) | static `FLOW_DECISIONS` | Add a `decisionsByDay` series to `GET /dashboard/summary`, or aggregate `GET /audit/recent` client-side |
| Recent Alerts | static `RECENT_ALERTS` | Not yet a backend endpoint — natural fit for a new `GET /audit/alerts` or filtering `GET /audit/recent` for `DENY` decisions |
| Top Data Flows by Volume | static `TOP_FLOWS` | Aggregate `GET /edges` + `GET /audit/recent` by source→destination, or add a dedicated aggregate endpoint |
| AI Classification Overview (donut) | static `aiDonutData` | Add a summary endpoint alongside `POST /ai/classify`, or count local state after listing classifications (no `GET /ai/classifications` list endpoint exists yet — add one if needed) |
| Recent Activity | `useDashboardData()` → `auditApi.recent(10)`, falls back to static demo rows if the call fails or returns empty | `GET /audit/recent` (already wired) |

`useDashboardData` (`src/hooks/useDashboardData.js`) polls
`/dashboard/summary` and `/audit/recent` every 30 seconds and **falls back
to demo numbers if the backend isn't reachable**, so the dashboard never
looks broken during development — swap in real endpoints incrementally,
panel by panel, using the table above.

---

## 6. Adding a new page end to end

Example: building out the **Services** page (currently a placeholder).

1. The API client already exists: `src/api/services.js` exports
   `servicesApi` (`list`, `create`, `update`, `remove`) and `edgesApi`.
2. Copy the pattern in `src/pages/Policies.jsx`:
   - `useState` for the list, loading, and error.
   - `useEffect` to call `servicesApi.list()` on mount.
   - A form that calls `servicesApi.create(...)` then reloads the list.
   - A table row per service with a delete button calling `servicesApi.remove(id)`.
3. Replace the `<PlaceholderPage />` route in `src/App.jsx` with your new
   `<Services />` component.

Every other nav item (`Data Flows`, `Runtime Monitor`, `Lineage`,
`AI Classification`, `Reports`, `Alerts`, `Users & Roles`, `Settings`)
follows the same recipe against its matching `src/api/*.js` file.

---

## 7. Environment variables

**Backend** (`backend/.env`, see `backend/.env.example`):

```
DB_URL, DB_USERNAME, DB_PASSWORD
REDIS_HOST, REDIS_PORT, REDIS_ENABLED
KAFKA_BOOTSTRAP_SERVERS, KAFKA_ENABLED
JWT_SECRET, JWT_EXPIRATION_MS
AI_SERVICE_URL
POLICY_DEFAULT_DECISION
SERVER_PORT
```

**Frontend** (`frontend/.env`, see `frontend/.env.example`):

```
VITE_API_BASE_URL=http://localhost:8080/api/v1
```

That's the only variable the frontend needs — everything else (JWT
handling, error shape, roles) is derived from what the backend returns.

---

## 8. CORS

The backend's `CorsConfig` allows all origins by default
(`policymesh.cors.allowed-origins=*`), which is fine for local development.
For a real deployment, set it to the frontend's actual origin:

```
POLICYMESH_CORS_ALLOWED_ORIGINS=https://app.yourcompany.com
```

---

## 9. Deploying together

- **Same host, different ports**: run the backend on `:8080` and serve the
  built frontend (`npm run build` → `dist/`) from any static host or
  behind Nginx, with `VITE_API_BASE_URL` pointing at the backend's public
  URL.
- **Single reverse proxy**: put Nginx/Traefik in front of both, routing
  `/api/*` to the backend container and everything else to the frontend's
  static files — then `VITE_API_BASE_URL` can just be `/api/v1` (same
  origin, no CORS needed at all).
- **Docker Compose**: the backend's `docker-compose.yml` already runs
  Postgres/Redis/Kafka/backend together; add a `frontend` service there
  that builds `frontend/Dockerfile` (a simple `nginx:alpine` serving
  `dist/`) if you want one `docker compose up` to bring up the whole stack.

---

## 10. What's real vs. placeholder in this frontend build

**Fully wired to the backend:**
- Auth (register/login/logout, JWT persisted, auto-redirect on 401)
- Dashboard stat cards + Recent Activity (via `/dashboard/summary`, `/audit/recent`)
- Policies page (full CRUD against `/policies`)

**Static/demo data, ready to wire (see section 5 & 6 above):**
- Policy Status donut, Data Flow Decisions chart, Recent Alerts, Top Data
  Flows, AI Classification donut
- Services, Data Flows, Runtime Monitor, Lineage, AI Classification,
  Reports, Alerts, Users & Roles, Settings pages (all `PlaceholderPage`
  stubs with the right title/subtitle, ready for the CRUD pattern used in
  `Policies.jsx`)

This split is intentional: the visual design and the plumbing (auth,
error handling, API client) are both done, so the remaining work is
"copy the Policies.jsx pattern" for each additional resource, not
inventing new architecture.
