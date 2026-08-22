import { useEffect, useState } from "react";
import { FileText, ShieldCheck, GitBranch, ShieldAlert, Gauge, RefreshCw, Loader2 } from "lucide-react";
import Topbar from "../components/layout/Topbar";
import StatCard from "../components/dashboard/StatCard";
import { DashboardCard, CardFooterLink } from "../components/dashboard/DashboardCard";
import DonutStat from "../components/dashboard/DonutStat";
import FlowDecisionsChart from "../components/dashboard/FlowDecisionsChart";
import ChartLegend from "../components/dashboard/ChartLegend";
import TopFlowsList from "../components/dashboard/TopFlowsList";
import AlertsList from "../components/dashboard/AlertsList";
import ActivityList from "../components/dashboard/ActivityList";
import { useDashboardData } from "../hooks/useDashboardData";
import { useAuth } from "../context/AuthContext";
import { policiesApi, servicesApi, edgesApi, auditApi } from "../api";

function toTitleCase(value) {
  return value.toLowerCase().split("_").map((w) => w[0]?.toUpperCase() + w.slice(1)).join(" ");
}

function relativeTime(ts) {
  if (!ts) return "";
  const diff = Date.now() - new Date(ts).getTime();
  const mins = Math.floor(diff / 60000);
  if (mins < 1) return "just now";
  if (mins < 60) return `${mins}m ago`;
  const hrs = Math.floor(mins / 60);
  if (hrs < 24) return `${hrs}h ago`;
  return `${Math.floor(hrs / 24)}d ago`;
}

function buildDecisionChart(decisions) {
  const days = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];
  const today = new Date();
  const map = {};
  for (let i = 6; i >= 0; i--) {
    const d = new Date(today);
    d.setDate(today.getDate() - i);
    const key = days[d.getDay()];
    map[key] = { day: key, allowed: 0, blocked: 0 };
  }
  for (const dec of decisions) {
    if (!dec.createdAt) continue;
    const d = new Date(dec.createdAt);
    const diff = Math.floor((today - d) / 86400000);
    if (diff > 6) continue;
    const key = days[d.getDay()];
    if (!map[key]) continue;
    if (dec.decision === "ALLOW") map[key].allowed++;
    else map[key].blocked++;
  }
  return Object.values(map);
}

function buildPolicyDonut(policies) {
  const counts = { ACTIVE: 0, DRAFT: 0, UNDER_REVIEW: 0, INACTIVE: 0 };
  for (const p of policies) {
    const s = p.status?.toUpperCase();
    if (s in counts) counts[s]++;
    else counts.INACTIVE++;
  }
  const total = policies.length || 1;
  return [
    { name: "Active", value: counts.ACTIVE, pct: Math.round((counts.ACTIVE / total) * 100), color: "#22c55e" },
    { name: "Draft", value: counts.DRAFT, pct: Math.round((counts.DRAFT / total) * 100), color: "#3b82f6" },
    { name: "Under Review", value: counts.UNDER_REVIEW, pct: Math.round((counts.UNDER_REVIEW / total) * 100), color: "#f59e0b" },
    { name: "Inactive", value: counts.INACTIVE, pct: Math.round((counts.INACTIVE / total) * 100), color: "#5b6478" },
  ];
}

function buildTopFlows(decisions, services) {
  const svcMap = Object.fromEntries((services || []).map((s) => [s.name, s]));
  const counts = {};
  for (const d of decisions) {
    const key = `${d.sourceService}→${d.destinationService}`;
    counts[key] = (counts[key] || 0) + 1;
  }
  return Object.entries(counts)
    .sort((a, b) => b[1] - a[1])
    .slice(0, 5)
    .map(([key, count]) => {
      const [source, destination] = key.split("→");
      return { source, destination, count };
    });
}

function buildAlerts(decisions) {
  return decisions
    .filter((d) => d.decision === "DENY")
    .slice(0, 4)
    .map((d) => ({
      severity: "High",
      message: `Blocked: ${d.dataClass} from ${d.sourceService} to ${d.destinationService}`,
      source: d.policyId || "Runtime Enforcement",
      time: relativeTime(d.createdAt),
    }));
}

function mapActivity(decisions) {
  return decisions.slice(0, 4).map((d) => ({
    type: d.decision === "ALLOW" ? "flow" : "alert",
    message: `${d.decision === "ALLOW" ? "Allowed" : "Blocked"}: ${d.sourceService} → ${d.destinationService} (${d.dataClass})`,
    actor: d.policyId || "Runtime Engine",
    time: relativeTime(d.createdAt),
  }));
}

export default function Dashboard() {
  const { user } = useAuth();
  const { summary, loading: summaryLoading } = useDashboardData();

  const [policies, setPolicies] = useState([]);
  const [decisions, setDecisions] = useState([]);
  const [services, setServices] = useState([]);
  const [detailsLoading, setDetailsLoading] = useState(true);
  const [lastRefresh, setLastRefresh] = useState(null);

  async function loadDetails() {
    setDetailsLoading(true);
    try {
      const [pol, dec, svc] = await Promise.allSettled([
        policiesApi.list(),
        auditApi.recent(200),
        servicesApi.list(),
      ]);
      if (pol.status === "fulfilled") setPolicies(pol.value);
      if (dec.status === "fulfilled") setDecisions(dec.value);
      if (svc.status === "fulfilled") setServices(svc.value);
      setLastRefresh(new Date());
    } finally {
      setDetailsLoading(false);
    }
  }

  useEffect(() => {
    loadDetails();
    const interval = setInterval(loadDetails, 30000);
    return () => clearInterval(interval);
  }, []);

  const policyDonutData = buildPolicyDonut(policies);
  const flowChart = buildDecisionChart(decisions);
  const topFlows = buildTopFlows(decisions, services);
  const alerts = buildAlerts(decisions);
  const activity = mapActivity(decisions);

  const activePolicies = policies.filter((p) => p.status === "ACTIVE").length;

  const displayName = user?.email?.split("@")[0] || (user?.role ? toTitleCase(user.role) : "User");

  return (
    <div className="pb-2">
      <Topbar
        title={`Welcome back, ${displayName} 👋`}
        subtitle="Here's what's happening with your data governance today."
      />

      <div className="flex items-center justify-end gap-3 px-6 lg:px-8 -mt-2 mb-2">
        <button
          onClick={loadDetails}
          disabled={detailsLoading}
          className="flex items-center gap-1.5 text-xs text-[var(--color-text-faint)] hover:text-white transition-colors"
        >
          <RefreshCw size={12} className={detailsLoading ? "animate-spin" : ""} />
          {lastRefresh ? `Updated ${relativeTime(lastRefresh)}` : "Loading…"}
        </button>
      </div>

      {/* Stat cards */}
      <div className="grid grid-cols-2 md:grid-cols-3 xl:grid-cols-5 gap-4 px-6 lg:px-8 mt-4">
        <StatCard icon={FileText} color="violet" label="Total Policies" value={summaryLoading ? "—" : summary.totalPolicies} delta={null} trend="up" />
        <StatCard icon={ShieldCheck} color="blue" label="Active Policies" value={detailsLoading ? "—" : activePolicies} delta={null} trend="up" />
        <StatCard icon={GitBranch} color="green" label="Flows Allowed" value={summaryLoading ? "—" : summary.allowedTransfers.toLocaleString()} delta={null} trend="up" />
        <StatCard icon={ShieldAlert} color="red" label="Flows Blocked" value={summaryLoading ? "—" : summary.blockedTransfers} delta={null} trend="down" />
        <StatCard icon={Gauge} color="amber" label="Compliance Score" value={summaryLoading ? "—" : `${summary.complianceScore}%`} delta={null} trend="up" />
      </div>

      {/* Row 2 */}
      <div className="grid grid-cols-1 xl:grid-cols-3 gap-4 px-6 lg:px-8 mt-4">
        <DashboardCard title="Policy Status Overview">
          {detailsLoading && policies.length === 0 ? (
            <div className="flex items-center justify-center h-32 text-[var(--color-text-faint)]">
              <Loader2 size={18} className="animate-spin" />
            </div>
          ) : (
            <DonutStat data={policyDonutData} total={policies.length} totalLabel="Policies" />
          )}
          <CardFooterLink to="/policies">View all policies</CardFooterLink>
        </DashboardCard>

        <DashboardCard
          title="Data Flow Decisions (This Week)"
          action={<ChartLegend items={[{ label: "Allowed", color: "#22c55e" }, { label: "Blocked", color: "#ef4444" }]} />}
        >
          {detailsLoading && decisions.length === 0 ? (
            <div className="flex items-center justify-center h-32 text-[var(--color-text-faint)]">
              <Loader2 size={18} className="animate-spin" />
            </div>
          ) : (
            <FlowDecisionsChart data={flowChart} />
          )}
          <CardFooterLink to="/runtime-monitor">View runtime monitor</CardFooterLink>
        </DashboardCard>

        <DashboardCard title="Recent Blocked Flows" action={<CardFooterLink to="/alerts">View all</CardFooterLink>}>
          {detailsLoading && decisions.length === 0 ? (
            <div className="flex items-center justify-center h-32 text-[var(--color-text-faint)]">
              <Loader2 size={18} className="animate-spin" />
            </div>
          ) : alerts.length === 0 ? (
            <div className="flex flex-col items-center justify-center h-24 gap-2">
              <ShieldCheck size={22} className="text-[var(--color-good)]" />
              <p className="text-xs text-[var(--color-text-faint)]">No blocked flows — all clear!</p>
            </div>
          ) : (
            <AlertsList alerts={alerts} />
          )}
        </DashboardCard>
      </div>

      {/* Row 3 */}
      <div className="grid grid-cols-1 xl:grid-cols-3 gap-4 px-6 lg:px-8 mt-4 mb-8">
        <DashboardCard title="Top Data Flows by Volume">
          {detailsLoading && decisions.length === 0 ? (
            <div className="flex items-center justify-center h-32 text-[var(--color-text-faint)]">
              <Loader2 size={18} className="animate-spin" />
            </div>
          ) : topFlows.length === 0 ? (
            <div className="flex items-center justify-center h-24">
              <p className="text-xs text-[var(--color-text-faint)]">No flow data yet.</p>
            </div>
          ) : (
            <TopFlowsList flows={topFlows} />
          )}
          <CardFooterLink to="/data-flows">View all data flows</CardFooterLink>
        </DashboardCard>

        <DashboardCard title="Services Overview">
          {detailsLoading && services.length === 0 ? (
            <div className="flex items-center justify-center h-32 text-[var(--color-text-faint)]">
              <Loader2 size={18} className="animate-spin" />
            </div>
          ) : (
            <div className="space-y-2 py-2">
              <div className="flex items-center justify-between text-sm">
                <span className="text-[var(--color-text-dim)]">Total Services</span>
                <span className="font-semibold text-white">{summary.totalServices}</span>
              </div>
              <div className="flex items-center justify-between text-sm">
                <span className="text-[var(--color-text-dim)]">Total Decisions Today</span>
                <span className="font-semibold text-white">{summary.decisionsToday}</span>
              </div>
              <div className="flex items-center justify-between text-sm">
                <span className="text-[var(--color-text-dim)]">Active Violations</span>
                <span className={`font-semibold ${summary.activeViolations > 0 ? "text-[var(--color-bad)]" : "text-[var(--color-good)]"}`}>
                  {summary.activeViolations}
                </span>
              </div>
              <div className="flex items-center justify-between text-sm">
                <span className="text-[var(--color-text-dim)]">Lineage Chain</span>
                <span className={`font-semibold ${summary.lineageValid ? "text-[var(--color-good)]" : "text-[var(--color-bad)]"}`}>
                  {summary.lineageValid ? "Valid ✓" : "Broken ✗"}
                </span>
              </div>
            </div>
          )}
          <CardFooterLink to="/services">Manage services</CardFooterLink>
        </DashboardCard>

        <DashboardCard title="Recent Activity" action={<CardFooterLink to="/lineage">View lineage</CardFooterLink>}>
          {detailsLoading && decisions.length === 0 ? (
            <div className="flex items-center justify-center h-32 text-[var(--color-text-faint)]">
              <Loader2 size={18} className="animate-spin" />
            </div>
          ) : activity.length === 0 ? (
            <div className="flex items-center justify-center h-24">
              <p className="text-xs text-[var(--color-text-faint)]">No recent activity.</p>
            </div>
          ) : (
            <ActivityList activities={activity} />
          )}
        </DashboardCard>
      </div>
    </div>
  );
}
