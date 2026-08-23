# PolicyMesh — Authoritative Frontend Design System & Architecture Specification

> **Version:** 2.0.0  
> **Status:** Production / Definitive Reference  
> **Design Philosophy:** Clean, premium, modern enterprise SaaS UI with excellent spacing, strict typography hierarchy, compact information density, and first-class light/dark theme support.

---

## 1. Design System Overview

PolicyMesh is an enterprise zero-trust data governance and residency enforcement platform. The user experience is engineered to deliver high-density intelligence with zero clutter:
- **Clarity over Complexity:** High contrast typography, clear visual boundaries, and minimal noise.
- **Immediate State Comprehension:** Semantic color codes (Green = Compliant/Active, Red = Violation/Blocked, Amber = Under Review/Pending, Blue = Informational, Purple = Brand).
- **Dual-Theme Parity:** Native first-class Light mode and true dark Charcoal mode (not plain inverted blacks).
- **Compact Density:** Ergonomic padding, compact card headers, and efficient data tables.

```
┌────────────────────────────────────────────────────────────────────────┐
│                              POLICYMESH                                │
│                          Govern. Enforce. Trust.                       │
└──────────────────────────────────┬─────────────────────────────────────┘
                                   │
       ┌───────────────────────────┴───────────────────────────┐
       ▼                                                       ▼
┌───────────────────────────────┐               ┌───────────────────────────────┐
│     LIGHT THEME (Default)     │               │          DARK THEME           │
│ Background: #f8fafc (Slate-50)│               │ Background: #0b0e14 (Charcoal)│
│ Surface:    #ffffff (White)   │               │ Surface:    #12161f (Slate-900│
│ Surface 2:  #f1f5f9 (Slate-100│               │ Surface 2:  #1a202c (Slate-800│
│ Border:     #e2e8f0 (Slate-200│               │ Border:     #232939 (Slate-700│
│ Text:       #0f172a (Slate-900│               │ Text:       #f3f4f6 (Gray-100)│
│ Brand:      #6366f1 (Indigo)  │               │ Brand:      #7c6ef8 (Purple)  │
└───────────────────────────────┘               └───────────────────────────────┘
```

---

## 2. Design Tokens & Semantic Variables

All styling uses standard semantic CSS variables defined in `frontend/src/index.css`:

### 2.1 Colors & Surfaces

| Token | Light Theme Value | Dark Theme Value | Purpose |
| :--- | :--- | :--- | :--- |
| `--color-bg` | `#f8fafc` (slate-50) | `#0b0e14` (deep charcoal) | Root page background |
| `--color-surface` | `#ffffff` (white) | `#12161f` (slate-900 surface) | Primary cards, panels, modals |
| `--color-surface-2` | `#f1f5f9` (slate-100) | `#1a202c` (slate-800 surface) | Table headers, dropdowns, hover states |
| `--color-surface-3` | `#e2e8f0` (slate-200) | `#232939` (card borders/dividers) | Inset inputs, tabs, secondary pills |
| `--color-border` | `#e2e8f0` (subtle border) | `#232939` (dark border) | Grid boundaries, card outlines |
| `--color-border-strong` | `#cbd5e1` (slate-300) | `#333b4f` (active border) | Focused inputs, hovered cards |

### 2.2 Typography & Text Tiers

| Token | Light Theme Value | Dark Theme Value | Purpose |
| :--- | :--- | :--- | :--- |
| `--color-text` | `#0f172a` (slate-900) | `#f3f4f6` (gray-100) | Primary headings, values, labels |
| `--color-text-dim` | `#475569` (slate-600) | `#9ca3af` (gray-400) | Subtitles, body descriptions, table cells |
| `--color-text-faint` | `#94a3b8` (slate-400) | `#5b6478` (slate-500) | Timestamps, helper notes, inactive icons |

### 2.3 Brand & Semantic Feedback Tokens

| Semantic Role | Token Variable | Hex Value | Used For |
| :--- | :--- | :--- | :--- |
| **Brand Primary** | `--color-brand` | `#6366f1` / `#7c6ef8` | Primary buttons, active nav items, charts |
| **Brand Light** | `--color-brand-light` | `rgba(99,102,241,0.12)` | Active nav item background, focus rings |
| **Good / Success** | `--color-good` | `#10b981` (emerald-500) | Allowed flows, active policies, valid chain |
| **Bad / Danger** | `--color-bad` | `#ef4444` (rose-500) | Blocked flows, violations, failed gates |
| **Warn / Review** | `--color-warn` | `#f59e0b` (amber-500) | Pending review, draft policies, degraded state |
| **Info / General** | `--color-info` | `#3b82f6` (blue-500) | Regional tags, low priority alerts |

---

## 3. Core Component Primitives

The UI is built on atomic, reusable components located in `frontend/src/components/ui/`:

### 3.1 Button (`Button.jsx`)
- **Variants:** `primary` (solid purple gradient), `secondary` (surface with border), `danger` (rose tone), `ghost` (subtle hover).
- **Sizes:** `sm` (compact 28px), `md` (standard 34px), `lg` (prominent 42px).
- **States:** Built-in `loading` spinner and icon integration.

### 3.2 Badge (`Badge.jsx`)
- Automatic variant resolution for compliance states: `good`, `bad`, `warn`, `info`, `brand`, `neutral`.
- Optional animated `dot` indicator and icon prefix.

### 3.3 Card (`Card.jsx` & `DashboardCard.jsx`)
- Elevation: Crisp border with subtle multi-layer shadow (`0 1px 3px rgba(0,0,0,0.05)`).
- Subcomponents: `CardHeader`, `CardTitle`, `CardBody`, `CardFooterLink`.

### 3.4 Modal & Drawer (`Modal.jsx`)
- Glassmorphism backdrop blur (`backdrop-blur-sm`).
- Keyboard escape listener (`Esc`) and outside-click handler.

### 3.5 Tabs (`Tabs.jsx`)
- Segmented pill navigation with dynamic item counters.

### 3.6 SearchableCombobox (`SearchableCombobox.jsx`)
- Fuzzy searchable dropdown with custom option creation, keyboard navigation (ArrowUp/ArrowDown/Enter), and automatic region metadata binding.

### 3.7 Pagination (`Pagination.jsx`)
- Dynamic page jump buttons (`1, 2, 3... 10`) with rows-per-page selector.

### 3.8 EmptyState & LoadingSkeleton (`EmptyState.jsx`, `LoadingSkeleton.jsx`)
- Consistent fallback UI with illustration icons and action triggers.

---

## 4. Application Layout Anatomy

The application shell (`AppShell.jsx`) enforces a standardized three-pane structure:

```
┌──────────────┬─────────────────────────────────────────────────────────────────┐
│              │ Topbar: Page Title | Search (Ctrl+K) | Theme | Alerts | Profile │
│              ├─────────────────────────────────────────────────────────────────┤
│              │                                                                 │
│   SIDEBAR    │                                                                 │
│              │                      SCROLLABLE VIEWPORT                        │
│ • Logo       │                      (Dashboard, Policies, etc.)                │
│ • 13 Nav     │                                                                 │
│   Links      │                                                                 │
│ • User Role  │                                                                 │
│ • Collapse   ├─────────────────────────────────────────────────────────────────┤
│              │ StatusBar: ● All systems operational | Subsystem Badges | v1.0.0│
└──────────────┴─────────────────────────────────────────────────────────────────┘
```

1. **Sidebar (`Sidebar.jsx`):**
   - Brand logo + title ("PolicyMesh") + tagline ("Govern. Enforce. Trust.").
   - 13 navigation links with soft purple active state (`bg-[var(--color-brand-light)] text-[var(--color-brand-text)]`).
   - Collapsible desktop view and responsive mobile slide-out drawer.
   - User profile footer card with avatar initials and role title.

2. **Topbar (`Topbar.jsx`):**
   - Page breadcrumb title and greeting ("Welcome back, Compliance Officer 👋").
   - Global Search input (`Ctrl+K`) opening the instant search modal.
   - Theme toggle button (Sun / Moon / Monitor).
   - Alerts bell badge.
   - User profile dropdown with logout and settings navigation.
   - Action slot for page-level actions (e.g., "+ New Policy", "Export CSV").

3. **StatusBar (`StatusBar.jsx`):**
   - Real-time telemetry health indicators for API, AI Service, Database, and Kafka.
   - Version tag ("PolicyMesh v1.0.0").

---

## 5. Page Specifications & Features

### 5.1 Dashboard (`Dashboard.jsx`)
- **5 KPI Summary Cards:** Total Policies, Active Policies, Data Flows Checked, Blocked Flows, Compliance Score (%).
- **Row 1 (3 Columns):** Policy Status Donut Chart, Flow Decisions Line Graph (Mon–Sun spline), Recent Alerts list.
- **Row 2 (3 Columns):** Top Data Flows by Volume (horizontal meters), AI Classification Donut, Recent Activity timeline.

### 5.2 Policies (`Policies.jsx`)
- Full DataTable with Policy Code, Name, Jurisdiction, Data Class, Allowed/Denied Regions, Status Toggle, and Delete.
- Interactive Search, Jurisdiction filter, and Status filter.
- Create Policy Modal and Declarative YAML Import Modal.

### 5.3 Services (`Services.jsx`)
- Service registry cataloging service names, deployment regions, and environment tiers.
- Add / Edit Service Modal with automatic region binding.

### 5.4 Data Flows (`DataFlows.jsx`)
- Data flow topology graph table with real-time compliance violation indicators.
- **"Re-evaluate Graph"** end-to-end trigger with live `Total Violations Found: {n}` pill.
- Add Edge modal with multi-select data sensitivity classes.

### 5.5 Runtime Monitor (`RuntimeMonitor.jsx`)
- Live AST evaluation simulation form with automatic region synchronization.
- Real-time decision banner with evaluation latency (&lt; 2ms).
- Live decision stream audit log with expandable cryptographic hash proofs.

### 5.6 Cryptographic Lineage (`Lineage.jsx`)
- Immutable SHA-256 hash-chain verification banner ("Hash Chain Valid: 100% verified").
- Ledger stream with sequence index, parent hash, and copyable hash utilities.

### 5.7 AI Sensitivity Classification (`AiClassification.jsx`)
- NLP field sensitivity tokenizer with confidence rating meter.
- Human-in-the-loop review table with Approve and Reject actions.

### 5.8 CI/CD Compliance Check (`CiCheck.jsx`)
- Pre-merge compliance scanner with Git branch combobox and SHA presets (`HEAD`, `HEAD~1`).
- Real-time regex validation and Pass/Blocked visual gate report.

### 5.9 Security Alerts (`Alerts.jsx`)
- Real-time violation stream with severity-colored icon badges (High, Medium, Low).
- Filter by All, Blocked, or Allowed events.

### 5.10 Compliance Reports (`Reports.jsx`)
- 4 Executive KPI cards and CSV export generation.
- Policy enforcement breakdown table with compliance progress bars.

### 5.11 System Status (`SystemStatus.jsx`)
- Subsystem health cards for REST API, AI Service, MySQL, Redis, Kafka, and Ledger Engine.
- Platform runtime specifications (JVM, Python, OS).

### 5.12 Users & Roles (`UsersRoles.jsx`)
- User Directory tab with role management and invitation modal.
- RBAC Permissions Matrix tab mapping entitlements across Admin, Compliance Officer, Engineer, and Viewer.

### 5.13 Settings (`Settings.jsx`)
- Profile & credentials card with change password form.
- Visual Theme Selector (Light, Dark, System Synchronized).
- Engine integration status card.

### 5.14 Auth (`Login.jsx`, `Register.jsx`)
- Clean authentication forms with corner theme toggle and password validation.

---

## 6. Theme Engine & Persistence

The theme engine is orchestrated by `ThemeContext.jsx`:
- **Modes:** `light`, `dark`, and `system`.
- **System Sync:** Listens to `window.matchMedia("(prefers-color-scheme: dark)")` in real time.
- **Persistence:** Saved in `localStorage.getItem("policymesh_theme")`.
- **Zero Flash:** Root `html` class `.dark` is applied synchronously before render.

---

## 7. Quality & Verification Standards

- **Build Pipeline:** `npm run build` generates production bundle in `< 1.5s` with zero errors.
- **Automated Tests:** All unit & integration suites (`MasterInputOutputAuditTest.java`, Pytest, CI Checker) pass with 100% success rate.
