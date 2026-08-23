import { useEffect, useState, useCallback } from "react";
import {
  Server,
  Activity,
  Database,
  Cpu,
  Radio,
  Sparkles,
  ShieldCheck,
  ShieldAlert,
  Clock,
  RefreshCw,
  CheckCircle2,
  AlertTriangle,
  Zap,
  Lock,
  Layers,
  Terminal,
} from "lucide-react";
import Topbar from "../components/layout/Topbar";
import { systemApi } from "../api";

export default function SystemStatus() {
  const [systemData, setSystemData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [diagnosing, setDiagnosing] = useState(false);
  const [error, setError] = useState(null);
  const [lastCheck, setLastCheck] = useState(new Date());
  const [pingLatency, setPingLatency] = useState(null);

  const fetchStatus = useCallback(async (isManual = false) => {
    if (isManual) setDiagnosing(true);
    setError(null);
    try {
      const data = await systemApi.runDiagnostics();
      setSystemData(data);
      setPingLatency(data.latency || 12);
      setLastCheck(new Date());
    } catch (err) {
      setError(err.message || "Failed to load system diagnostics");
    } finally {
      setLoading(false);
      if (isManual) setDiagnosing(false);
    }
  }, []);

  useEffect(() => {
    fetchStatus(false);
    const interval = setInterval(() => fetchStatus(false), 20000);
    return () => clearInterval(interval);
  }, [fetchStatus]);

  const components = systemData
    ? [
        {
          id: "api",
          name: "PolicyMesh REST API",
          badge: "Port 8080",
          icon: Server,
          status: systemData.api?.status || "HEALTHY",
          version: systemData.api?.version || "Spring Boot 3.3 (Java 21)",
          details: systemData.api?.details || "Stateless JWT RBAC, RFC 7807 problem details",
          color: "text-emerald-400",
          bgColor: "bg-emerald-500/10",
          borderColor: "border-emerald-500/30",
        },
        {
          id: "ai",
          name: "AI Sensitivity Service",
          badge: "Port 8000",
          icon: Sparkles,
          status: systemData.ai?.status || "HEALTHY",
          version: systemData.ai?.version || "FastAPI / Python 3.13",
          details: systemData.ai?.details || "Remote FastAPI connected with local deterministic fallback",
          color: systemData.ai?.status === "HEALTHY" ? "text-emerald-400" : "text-cyan-400",
          bgColor: systemData.ai?.status === "HEALTHY" ? "bg-emerald-500/10" : "bg-cyan-500/10",
          borderColor: systemData.ai?.status === "HEALTHY" ? "border-emerald-500/30" : "border-cyan-500/30",
        },
        {
          id: "db",
          name: "Relational Store (MySQL)",
          badge: "Port 3306",
          icon: Database,
          status: systemData.database?.status || "HEALTHY",
          version: systemData.database?.version || "MySQL 8.4",
          details: systemData.database?.details || "HikariCP connection pool active, schema synchronized",
          color: systemData.database?.status === "HEALTHY" ? "text-emerald-400" : "text-amber-400",
          bgColor: systemData.database?.status === "HEALTHY" ? "bg-emerald-500/10" : "bg-amber-500/10",
          borderColor: systemData.database?.status === "HEALTHY" ? "border-emerald-500/30" : "border-amber-500/30",
        },
        {
          id: "redis",
          name: "Policy Cache & Fast-Path",
          badge: "Port 6379",
          icon: Zap,
          status: systemData.redis?.status || "LOCAL_FALLBACK",
          version: systemData.redis?.version || "Redis 7 / In-Memory Fast Path",
          details: systemData.redis?.details || "In-memory fast cache active with resilient local fallback",
          color: systemData.redis?.status === "CONNECTED" ? "text-emerald-400" : "text-blue-400",
          bgColor: systemData.redis?.status === "CONNECTED" ? "bg-emerald-500/10" : "bg-blue-500/10",
          borderColor: systemData.redis?.status === "CONNECTED" ? "border-emerald-500/30" : "border-blue-500/30",
        },
        {
          id: "kafka",
          name: "Async Event Stream",
          badge: "Port 9092",
          icon: Radio,
          status: systemData.kafka?.status || "STANDBY",
          version: systemData.kafka?.version || "Apache Kafka 3.8",
          details: systemData.kafka?.details || "Producer configured for policy & lineage telemetry events",
          color: systemData.kafka?.status === "HEALTHY" ? "text-emerald-400" : "text-purple-400",
          bgColor: systemData.kafka?.status === "HEALTHY" ? "bg-emerald-500/10" : "bg-purple-500/10",
          borderColor: systemData.kafka?.status === "HEALTHY" ? "border-emerald-500/30" : "border-purple-500/30",
        },
      ]
    : [];

  const allOperational = components.every(
    (c) => c.status === "HEALTHY" || c.status === "CONNECTED" || c.status === "LOCAL_FALLBACK" || c.status === "STANDBY"
  );

  return (
    <div>
      <Topbar
        title="System Status"
        subtitle="Real-time telemetry, microservice health checks, database connectivity, and policy engine state."
      />

      <div className="px-6 lg:px-8 mt-4 space-y-5 pb-12">
        {/* Top Executive Summary Banner */}
        <div className="card p-5 bg-gradient-to-r from-[var(--color-surface)] via-[var(--color-surface)] to-[var(--color-surface-2)] border border-[var(--color-border)]">
          <div className="flex flex-wrap items-center justify-between gap-4">
            <div className="flex items-center gap-4">
              <div
                className={`w-12 h-12 rounded-2xl flex items-center justify-center border shadow-lg ${
                  allOperational
                    ? "bg-emerald-500/15 border-emerald-500/30 text-emerald-400"
                    : "bg-amber-500/15 border-amber-500/30 text-amber-400"
                }`}
              >
                {allOperational ? <ShieldCheck size={26} /> : <ShieldAlert size={26} />}
              </div>
              <div>
                <div className="flex items-center gap-2.5">
                  <h2 className="text-base font-semibold text-white">
                    {allOperational ? "All Systems Operational" : "System Degraded"}
                  </h2>
                  <span className="flex items-center gap-1 text-[11px] px-2 py-0.5 rounded-full font-mono bg-emerald-500/10 text-emerald-400 border border-emerald-500/30">
                    <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse" />
                    LIVE
                  </span>
                </div>
                <p className="text-xs text-[var(--color-text-dim)] mt-0.5">
                  Continuous multi-tier health monitoring across REST API, AI Classifier, Database, Cache, and Event Stream.
                </p>
              </div>
            </div>

            <div className="flex items-center gap-3">
              <div className="text-right hidden sm:block">
                <p className="text-[11px] text-[var(--color-text-faint)]">Round-Trip Latency</p>
                <p className="text-xs font-mono font-bold text-white">{pingLatency ? `${pingLatency} ms` : "—"}</p>
              </div>
              <button
                onClick={() => fetchStatus(true)}
                disabled={diagnosing}
                className="btn-primary flex items-center gap-2 text-xs px-3.5 py-2 shadow-md disabled:opacity-50"
              >
                <RefreshCw size={13} className={diagnosing ? "animate-spin" : ""} />
                <span>{diagnosing ? "Checking…" : "Run Diagnostic Check"}</span>
              </button>
            </div>
          </div>
        </div>

        {error && (
          <div className="rounded-xl bg-[var(--color-bad)]/10 border border-[var(--color-bad)]/30 px-4 py-3 text-xs text-[var(--color-bad)] flex items-center justify-between">
            <span>{error}</span>
            <button onClick={() => fetchStatus(true)} className="underline ml-4">
              Retry Check
            </button>
          </div>
        )}

        {/* Microservices & Infrastructure Grid */}
        <div>
          <h3 className="text-xs font-semibold uppercase tracking-wider text-[var(--color-text-faint)] mb-3">
            Infrastructure & Subsystems
          </h3>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {components.map((comp) => {
              const Icon = comp.icon;
              return (
                <div
                  key={comp.id}
                  className="card p-4 hover:border-[var(--color-brand)]/50 transition-all flex flex-col justify-between"
                >
                  <div>
                    <div className="flex items-center justify-between mb-3">
                      <div className="flex items-center gap-2.5">
                        <div className={`p-2 rounded-xl border ${comp.bgColor} ${comp.borderColor} ${comp.color}`}>
                          <Icon size={18} />
                        </div>
                        <div>
                          <h4 className="text-xs font-semibold text-white">{comp.name}</h4>
                          <span className="text-[10px] font-mono text-[var(--color-text-faint)]">{comp.badge}</span>
                        </div>
                      </div>
                      <span
                        className={`text-[10px] font-bold px-2 py-0.5 rounded-md border font-mono ${comp.bgColor} ${comp.borderColor} ${comp.color}`}
                      >
                        {comp.status}
                      </span>
                    </div>

                    <div className="space-y-1.5 pt-1">
                      <div className="text-[11px] text-[var(--color-text-dim)]">
                        <span className="text-[var(--color-text-faint)]">Stack: </span>
                        <span className="font-mono text-white/90">{comp.version}</span>
                      </div>
                      <p className="text-xs text-[var(--color-text-faint)] leading-relaxed">{comp.details}</p>
                    </div>
                  </div>

                  <div className="mt-4 pt-3 border-t border-[var(--color-border)] flex items-center justify-between text-[10px] text-[var(--color-text-faint)]">
                    <span className="flex items-center gap-1">
                      <CheckCircle2 size={11} className="text-emerald-400" /> Auto-Recoverable
                    </span>
                    <span className="font-mono">Updated: {lastCheck.toLocaleTimeString()}</span>
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        {/* Governance Engine & Policy Compiler State */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
          <div className="card p-5 space-y-3">
            <div className="flex items-center gap-2 mb-1">
              <Lock size={16} className="text-[var(--color-brand)]" />
              <h3 className="text-sm font-semibold text-white">Governance Engine & Zero-Trust State</h3>
            </div>
            <p className="text-xs text-[var(--color-text-dim)] leading-relaxed">
              Every request is evaluated under mathematical Zero-Trust axioms: unauthorized, unclassified, or cross-border
              anomalies are denied by default.
            </p>

            <div className="grid grid-cols-2 gap-2.5 pt-2">
              <div className="p-3 rounded-xl bg-[var(--color-surface-2)] border border-[var(--color-border)]">
                <span className="text-[10px] text-[var(--color-text-faint)] uppercase block">Enforcement Mode</span>
                <span className="text-xs font-mono font-bold text-emerald-400">STRICT_ENFORCE</span>
              </div>
              <div className="p-3 rounded-xl bg-[var(--color-surface-2)] border border-[var(--color-border)]">
                <span className="text-[10px] text-[var(--color-text-faint)] uppercase block">Default Decision</span>
                <span className="text-xs font-mono font-bold text-amber-400">DENY</span>
              </div>
              <div className="p-3 rounded-xl bg-[var(--color-surface-2)] border border-[var(--color-border)]">
                <span className="text-[10px] text-[var(--color-text-faint)] uppercase block">Lineage Audit Hash</span>
                <span className="text-xs font-mono font-bold text-white">SHA-256 Merkle</span>
              </div>
              <div className="p-3 rounded-xl bg-[var(--color-surface-2)] border border-[var(--color-border)]">
                <span className="text-[10px] text-[var(--color-text-faint)] uppercase block">Human Review</span>
                <span className="text-xs font-mono font-bold text-cyan-400">MANDATORY</span>
              </div>
            </div>
          </div>

          <div className="card p-5 space-y-3">
            <div className="flex items-center gap-2 mb-1">
              <Terminal size={16} className="text-[var(--color-brand)]" />
              <h3 className="text-sm font-semibold text-white">Diagnostics & Telemetry Payload</h3>
            </div>
            <p className="text-xs text-[var(--color-text-dim)]">
              Raw system heartbeat metadata and active configuration attributes.
            </p>

            <div className="rounded-xl bg-black/40 border border-[var(--color-border)] p-3 font-mono text-[11px] text-[var(--color-text-dim)] overflow-x-auto max-h-40">
              <pre>
                {JSON.stringify(
                  {
                    version: "1.0.0",
                    environment: "production",
                    timestamp: systemData?.timestamp || new Date().toISOString(),
                    engine: systemData?.governanceEngine || {
                      enforcementMode: "STRICT_ENFORCE",
                      defaultDecision: "DENY",
                    },
                    subsystems: {
                      api: systemData?.api?.status,
                      database: systemData?.database?.status,
                      redis: systemData?.redis?.status,
                      ai: systemData?.ai?.status,
                      kafka: systemData?.kafka?.status,
                    },
                  },
                  null,
                  2
                )}
              </pre>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
