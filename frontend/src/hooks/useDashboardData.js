import { useEffect, useState } from "react";
import { dashboardApi, auditApi } from "../api";

// Real empty baseline for newly registered users before data flows are registered.
const FALLBACK_SUMMARY = {
  complianceScore: 100,
  totalPolicies: 0,
  totalServices: 0,
  allowedTransfers: 0,
  blockedTransfers: 0,
  activeViolations: 0,
  recentDecisions: [],
};

export function useDashboardData() {
  const [summary, setSummary] = useState(FALLBACK_SUMMARY);
  const [recentActivity, setRecentActivity] = useState([]);
  const [loading, setLoading] = useState(true);
  const [usingFallback, setUsingFallback] = useState(false);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      setLoading(true);
      try {
        const [summaryData, activity] = await Promise.all([
          dashboardApi.summary(),
          auditApi.recent(10).catch(() => []),
        ]);
        if (!cancelled) {
          setSummary(summaryData);
          setRecentActivity(activity);
          setUsingFallback(false);
        }
      } catch (err) {
        // Backend not reachable yet — keep the dashboard usable with fallback data.
        if (!cancelled) {
          setUsingFallback(true);
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    load();
    const interval = setInterval(load, 30000);
    return () => {
      cancelled = true;
      clearInterval(interval);
    };
  }, []);

  return { summary, recentActivity, loading, usingFallback };
}
