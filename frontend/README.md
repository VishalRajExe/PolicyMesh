# PolicyMesh — Frontend

React + Vite + Tailwind CSS dashboard for the PolicyMesh backend.

## Quick start

```bash
npm install
cp .env.example .env       # point VITE_API_BASE_URL at your backend
npm run dev                # http://localhost:5173
```

## Build

```bash
npm run build
npm run preview
```

## Stack

- React 19 + Vite
- Tailwind CSS v4 (dark theme, tokens in `src/index.css`)
- React Router v6 for navigation
- Recharts for the donut/line charts
- lucide-react for icons
- Axios for the API client (`src/api/`)

## Structure

```
src/
  api/           one file per backend module (policies, services, enforcement, ci, lineage, ...)
  components/
    layout/      Sidebar, Topbar, StatusBar, AppShell, ProtectedRoute
    dashboard/   StatCard, DonutStat, FlowDecisionsChart, AlertsList, ActivityList, ...
  context/       AuthContext (JWT + current user)
  hooks/         useDashboardData (polls the backend, falls back to demo data offline)
  pages/         Dashboard, Policies, Login, Register, PlaceholderPage
```

See **design.md** at the repo root for exactly how this connects to the
Spring Boot backend, environment variables, and how to wire up the
remaining placeholder pages.
