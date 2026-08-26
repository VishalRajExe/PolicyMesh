import { useEffect, useState } from "react";
import {
  FileText,
  ShieldCheck,
  GitBranch,
  ShieldAlert,
  CheckCircle2,
  Upload,
  Plus,
  RefreshCw,
  Loader2,
  ChevronDown,
} from "lucide-react";
import { useNavigate } from "react-router-dom";
import Topbar from "../components/layout/Topbar";
import StatCard from "../components/dashboard/StatCard";
import { DashboardCard, CardFooterLink } from "../components/dashboard/DashboardCard";
import DonutStat from "../components/dashboard/DonutStat";
import FlowDecisionsChart from "../components/dashboard/FlowDecisionsChart";
import TopFlowsList from "../components/dashboard/TopFlowsList";
import AlertsList from "../components/dashboard/AlertsList";
import ActivityList from "../components/dashboard/ActivityList";
import Button from "../components/ui/Button";
import { useDashboardData } from "../hooks/useDashboardData";
import { useAuth } from "../context/AuthContext";
import { policiesApi, servicesApi, auditApi, aiApi, githubApi } from "../api";
import { GitCommit, GitPullRequest, ArrowRight, ShieldX } from "lucide-react";

function toTitleCase(value) {
  return value
    .toLowerCase()
    .split("_")
    .map((w) => w[0]?.toUpperCase() + w.slice(1))
    .join(" ");
}

function relativeTime(ts) {
  if (!ts) return "recently";
  const diff = Date.now() - new Date(ts).getTime();
  const mins = Math.floor(diff / 60000);
  if (mins < 1) return "just now";
  if (mins < 60) return `${mins}m ago`;
  const hrs = Math.floor(mins / 60);
  if (hrs < 24) return `${hrs}h ago`;
  return `${Math.floor(hrs / 24)}d ago`;
}

function formatComplianceScore(score) {
  if (score == null) return "0%";
  const num = typeof score === "number" ? score : parseFloat(score);
  if (isNaN(num)) return "0%";
  const pct = num <= 1 ? num * 100 : num;
  return pct % 1 === 0 ? `${Math.round(pct)}%` : `${pct.toFixed(1)}%`;
}

function buildDecisionChart(decisions) {
  const days = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"];
  const today = new Date();
  const map = {};

  for (let i = 6; i >= 0; i--) {
    const d = new Date(today);
    d.setDate(today.getDate() - i);
    const dayIndex = (d.getDay() + 6) % 7; // Monday = 0
    const key = days[dayIndex];
    map[key] = { day: key, allowed: 0, blocked: 0 };
  }

  for (const dec of decisions) {
    if (!dec.createdAt) continue;
    const d = new Date(dec.createdAt);
    const diff = Math.floor((today - d) / 86400000);
    if (diff > 6) continue;
    const dayIndex = (d.getDay() + 6) % 7;
    const key = days[dayIndex];
    if (!map[key]) continue;
    if (dec.decision === "ALLOW") map[key].allowed++;
    else map[key].blocked++;
  }

  return Object.values(map);
}

function buildPolicyDonut(policies) {
  const counts = { ACTIVE: 0, DRAFT: 0, UNDER_REVIEW: 0, INACTIVE: 0 };
  for (const p of policies) {
    const s = (p.status || "ACTIVE").toUpperCase();
    if (s in counts) counts[s]++;
    else counts.INACTIVE++;
  }

  const total = policies.length;
  if (total === 0) {
    return {
      total: 0,
      slices: [
        { name: "Active", value: 0, pct: 0, color: "#10b981" },
        { name: "Draft", value: 0, pct: 0, color: "#3b82f6" },
        { name: "Under Review", value: 0, pct: 0, color: "#f97316" },
        { name: "Inactive", value: 0, pct: 0, color: "#9ca3af" },
      ],
    };
  }

  return {
    total,
    slices: [
      { name: "Active", value: counts.ACTIVE, pct: Math.round((counts.ACTIVE / total) * 100), color: "#10b981" },
      { name: "Draft", value: counts.DRAFT, pct: Math.round((counts.DRAFT / total) * 100), color: "#3b82f6" },
      { name: "Under Review", value: counts.UNDER_REVIEW, pct: Math.round((counts.UNDER_REVIEW / total) * 100), color: "#f97316" },
      { name: "Inactive", value: counts.INACTIVE, pct: Math.round((counts.INACTIVE / total) * 100), color: "#9ca3af" },
    ],
  };
}

function buildAiDonut(aiList) {
  let approved = 0;
  let pending = 0;
  let rejected = 0;

  for (const c of aiList) {
    if (c.status === "APPROVED") approved++;
    else if (c.status === "PENDING") pending++;
    else rejected++;
  }

  const total = aiList.length;
  if (total === 0) {
    return {
      total: 0,
      slices: [
        { name: "Classified", value: 0, pct: 0, color: "#10b981" },
        { name: "Pending Review", value: 0, pct: 0, color: "#3b82f6" },
        { name: "Unclassified", value: 0, pct: 0, color: "#f97316" },
      ],
    };
  }

  return {
    total,
    slices: [
      { name: "Classified", value: approved, pct: Math.round((approved / total) * 100), color: "#10b981" },
      { name: "Pending Review", value: pending, pct: Math.round((pending / total) * 100), color: "#3b82f6" },
      { name: "Unclassified", value: rejected, pct: Math.round((rejected / total) * 100), color: "#f97316" },
    ],
  };
}

function buildTopFlows(decisions) {
  const counts = {};
  for (const d of decisions) {
    if (!d.sourceService || !d.destinationService) continue;
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
      message: `Blocked data flow: ${d.dataClass || "PII"} from ${d.sourceService} to ${d.destinationService}`,
      source: d.policyId || "Runtime Enforcement",
      time: relativeTime(d.createdAt),
    }));
}

function mapActivity(decisions) {
  return decisions.slice(0, 4).map((d) => ({
    type: d.decision === "ALLOW" ? "flow" : "approval",
    message: `Data flow ${d.decision === "ALLOW" ? "allowed" : "blocked"}: ${d.sourceService} → ${d.destinationService}`,
    actor: d.decision === "ALLOW" ? "Runtime Engine" : "Policy Enforcement",
    time: relativeTime(d.createdAt),
  }));
}

export default function Dashboard() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const { summary } = useDashboardData();

  const [policies, setPolicies] = useState([]);
  const [decisions, setDecisions] = useState([]);
  const [services, setServices] = useState([]);
  const [aiClassifications, setAiClassifications] = useState([]);
  const [recentCommits, setRecentCommits] = useState([]);
  const [webhookDeliveries, setWebhookDeliveries] = useState([]);
  const [loading, setLoading] = useState(true);

  async function loadAllData() {
    setLoading(true);
    try {
      const [pol, dec, svc, ai, cms, dlv] = await Promise.allSettled([
        policiesApi.list(),
        auditApi.recent(150),
        servicesApi.list(),
        aiApi.list(),
        githubApi.listCommits(0, 6),
        githubApi.listDeliveries(0, 6),
      ]);

      if (pol.status === "fulfilled") setPolicies(Array.isArray(pol.value) ? pol.value : []);
      if (dec.status === "fulfilled") setDecisions(Array.isArray(dec.value) ? dec.value : []);
      if (svc.status === "fulfilled") setServices(Array.isArray(svc.value) ? svc.value : []);
      if (ai.status === "fulfilled") setAiClassifications(Array.isArray(ai.value) ? ai.value : []);
      if (cms.status === "fulfilled" && cms.value?.content) {
        setRecentCommits(cms.value.content);
      }
      if (dlv.status === "fulfilled" && dlv.value?.content) {
        setWebhookDeliveries(dlv.value.content);
      }
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadAllData();
    const interval = setInterval(loadAllData, 30000);
    return () => clearInterval(interval);
  }, []);

  const policyDonut = buildPolicyDonut(policies);
  const aiDonut = buildAiDonut(aiClassifications);
  const flowChartData = buildDecisionChart(decisions);
  const topFlows = buildTopFlows(decisions);
  const alerts = buildAlerts(decisions);
  const activity = mapActivity(decisions);

  const totalPoliciesCount = policies.length > 0 ? policies.length : (summary?.totalPolicies || 0);
  const activePoliciesCount = policies.length > 0 ? policies.filter((p) => p.status === "ACTIVE").length : (summary?.activePolicies || 0);
  const flowsCheckedCount = decisions.length > 0 ? decisions.length : ((summary?.allowedTransfers || 0) + (summary?.blockedTransfers || 0));
  const blockedFlowsCount = decisions.length > 0 ? decisions.filter((d) => d.decision === "DENY").length : (summary?.blockedTransfers || 0);
  const complianceScore = flowsCheckedCount === 0 ? "0%" : `${Math.round(((flowsCheckedCount - blockedFlowsCount) / flowsCheckedCount) * 100)}%`;

  const now = Date.now();
  const sevenDaysMs = 7 * 86400 * 1000;
  const fourteenDaysMs = 14 * 86400 * 1000;

  function calculateTrend(items, filterFn = () => true) {
    if (!items || items.length === 0) {
      return { delta: "No previous data", trend: "neutral", subtitle: "" };
    }
    const filtered = items.filter(filterFn);
    if (filtered.length === 0) {
      return { delta: "No activity yet", trend: "neutral", subtitle: "" };
    }

    const currentWeekCount = filtered.filter((item) => {
      if (!item.createdAt) return false;
      const t = new Date(item.createdAt).getTime();
      return t >= now - sevenDaysMs;
    }).length;

    const previousWeekCount = filtered.filter((item) => {
      if (!item.createdAt) return false;
      const t = new Date(item.createdAt).getTime();
      return t >= now - fourteenDaysMs && t < now - sevenDaysMs;
    }).length;

    if (previousWeekCount === 0 && currentWeekCount === 0) {
      return { delta: "No previous data", trend: "neutral", subtitle: "" };
    }
    if (previousWeekCount === 0) {
      return { delta: `+${currentWeekCount}`, trend: "neutral", subtitle: "this week" };
    }

    const pct = Math.round(((currentWeekCount - previousWeekCount) / previousWeekCount) * 100);
    if (pct > 0) {
      return { delta: `↑ ${pct}%`, trend: "up", subtitle: "from last week" };
    } else if (pct < 0) {
      return { delta: `↓ ${Math.abs(pct)}%`, trend: "down", subtitle: "from last week" };
    }
    return { delta: "0%", trend: "neutral", subtitle: "vs last week" };
  }

  function calculateComplianceTrend() {
    if (flowsCheckedCount === 0 || decisions.length === 0) {
      return { delta: "No previous data", trend: "neutral", subtitle: "" };
    }
    const currentWeekDecisions = decisions.filter((d) => d.createdAt && new Date(d.createdAt).getTime() >= now - sevenDaysMs);
    const previousWeekDecisions = decisions.filter((d) => d.createdAt && new Date(d.createdAt).getTime() >= now - fourteenDaysMs && new Date(d.createdAt).getTime() < now - sevenDaysMs);

    if (previousWeekDecisions.length === 0) {
      return { delta: "No previous data", trend: "neutral", subtitle: "" };
    }

    const prevBlocked = previousWeekDecisions.filter((d) => d.decision === "DENY").length;
    const prevScore = Math.round(((previousWeekDecisions.length - prevBlocked) / previousWeekDecisions.length) * 100);
    const currBlocked = currentWeekDecisions.filter((d) => d.decision === "DENY").length;
    const currScore = currentWeekDecisions.length > 0 ? Math.round(((currentWeekDecisions.length - currBlocked) / currentWeekDecisions.length) * 100) : 0;
    const diff = currScore - prevScore;

    if (diff > 0) return { delta: `↑ ${diff}%`, trend: "up", subtitle: "from last week" };
    if (diff < 0) return { delta: `↓ ${Math.abs(diff)}%`, trend: "down", subtitle: "from last week" };
    return { delta: "0%", trend: "neutral", subtitle: "vs last week" };
  }

  const totalPoliciesTrend = calculateTrend(policies);
  const activePoliciesTrend = calculateTrend(policies, (p) => p.status === "ACTIVE");
  const flowsCheckedTrend = calculateTrend(decisions);
  const blockedFlowsTrend = calculateTrend(decisions, (d) => d.decision === "DENY");
  const complianceTrend = calculateComplianceTrend();

  const roleName = user?.role ? toTitleCase(user.role) : "Compliance Officer";

  const topbarActions = (
    <div className="flex items-center gap-2.5">
      <Button
        variant="secondary"
        size="md"
        icon={Upload}
        onClick={() => navigate("/policies?action=import")}
      >
        Import Policy
      </Button>
      <Button
        variant="primary"
        size="md"
        icon={Plus}
        onClick={() => navigate("/policies?action=new")}
      >
        New Policy
      </Button>
    </div>
  );

  return (
    <div className="min-h-full flex flex-col bg-[var(--color-bg)]">
      <Topbar alertCount={alerts.length} />

      <div className="flex-1 p-4 sm:p-6 lg:p-8 space-y-4 sm:space-y-6">
        {/* Row 1: 5 KPI Summary Cards */}
        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-3.5 sm:gap-4">
          <StatCard
            icon={FileText}
            color="purple"
            label="Total Policies"
            value={totalPoliciesCount}
            delta={totalPoliciesTrend.delta}
            trend={totalPoliciesTrend.trend}
            subtitle={totalPoliciesTrend.subtitle}
          />
          <StatCard
            icon={ShieldCheck}
            color="blue"
            label="Active Policies"
            value={activePoliciesCount}
            delta={activePoliciesTrend.delta}
            trend={activePoliciesTrend.trend}
            subtitle={activePoliciesTrend.subtitle}
          />
          <StatCard
            icon={GitBranch}
            color="green"
            label="Data Flows Checked"
            value={flowsCheckedCount.toLocaleString()}
            delta={flowsCheckedTrend.delta}
            trend={flowsCheckedTrend.trend}
            subtitle={flowsCheckedTrend.subtitle}
          />
          <StatCard
            icon={ShieldAlert}
            color="red"
            label="Blocked Flows"
            value={blockedFlowsCount}
            delta={blockedFlowsTrend.delta}
            trend={blockedFlowsTrend.trend}
            subtitle={blockedFlowsTrend.subtitle}
          />
          <StatCard
            icon={CheckCircle2}
            color="amber"
            label="Compliance Score"
            value={complianceScore}
            delta={complianceTrend.delta}
            trend={complianceTrend.trend}
            subtitle={complianceTrend.subtitle}
          />
        </div>

        {/* Row 2: 3-Column Layout (stacked on mobile/tablet) */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 sm:gap-5">
          {/* Policy Status Overview */}
          <DashboardCard title="Policy Status Overview">
            <DonutStat
              data={policyDonut.slices}
              total={policyDonut.total}
              totalLabel="Policies"
              size={135}
            />
            <CardFooterLink to="/policies">View all policies</CardFooterLink>
          </DashboardCard>

          {/* Data Flow Decisions */}
          <DashboardCard
            title="Data Flow Decisions (This Week)"
            action={
              <div className="flex items-center gap-1 text-[11px] text-[var(--color-text-faint)] font-medium">
                <span>This Week</span>
                <ChevronDown size={12} />
              </div>
            }
          >
            <FlowDecisionsChart data={flowChartData} />
            <CardFooterLink to="/runtime-monitor">View runtime monitor</CardFooterLink>
          </DashboardCard>

          {/* Recent Alerts */}
          <DashboardCard
            title="Recent Alerts"
            action={
              <button
                onClick={() => navigate("/alerts")}
                className="text-xs text-[var(--color-brand)] hover:underline font-medium"
              >
                View all →
              </button>
            }
          >
            <AlertsList alerts={alerts} />
          </DashboardCard>
        </div>

        {/* Row 3: 3-Column Layout (stacked on mobile/tablet) */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 sm:gap-5">
          {/* Top Data Flows by Volume */}
          <DashboardCard
            title="Top Data Flows by Volume"
            action={
              <div className="flex items-center gap-1 text-[11px] text-[var(--color-text-faint)] font-medium">
                <span>This Week</span>
                <ChevronDown size={12} />
              </div>
            }
          >
            <TopFlowsList flows={topFlows} />
            <CardFooterLink to="/data-flows">View all data flows</CardFooterLink>
          </DashboardCard>

          {/* AI Classification Overview */}
          <DashboardCard title="AI Classification Overview">
            <DonutStat
              data={aiDonut.slices}
              total={aiDonut.total}
              totalLabel="Total Fields"
              size={135}
            />
            <CardFooterLink to="/ai-classification">Go to AI Classification</CardFooterLink>
          </DashboardCard>

          {/* Recent Activity */}
          <DashboardCard
            title="Recent Activity"
            action={
              <button
                onClick={() => navigate("/lineage")}
                className="text-xs text-[var(--color-brand)] hover:underline font-medium"
              >
                View all →
              </button>
            }
          >
            <ActivityList activities={activity} />
          </DashboardCard>
        </div>
      </div>
    </div>
  );
}
