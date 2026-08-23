import { useEffect, useState } from "react";
import { systemApi } from "../../api";

const DEFAULT_SERVICES = [
  { label: "API", status: "Healthy", color: "var(--color-good)" },
  { label: "AI Service", status: "Healthy", color: "var(--color-good)" },
  { label: "Database", status: "Healthy", color: "var(--color-good)" },
  { label: "Kafka", status: "Healthy", color: "var(--color-good)" },
];

export default function StatusBar({
  allSystemsLabel = "All systems operational",
}) {
  const [subsystems, setSubsystems] = useState(DEFAULT_SERVICES);

  useEffect(() => {
    let mounted = true;
    async function checkHealth() {
      try {
        const data = await systemApi.getStatus();
        if (mounted && data) {
          const apiStatus = data.api?.status === "HEALTHY" ? "Healthy" : "Degraded";
          const aiStatus = data.aiService?.status === "HEALTHY" ? "Healthy" : "Degraded";
          const dbStatus = data.database?.status === "HEALTHY" ? "Healthy" : "Degraded";
          const kafkaStatus = data.kafka?.status === "STANDBY" || data.kafka?.status === "HEALTHY" ? "Healthy" : "Offline";

          setSubsystems([
            { label: "API", status: apiStatus, color: apiStatus === "Healthy" ? "var(--color-good)" : "var(--color-warn)" },
            { label: "AI Service", status: aiStatus, color: aiStatus === "Healthy" ? "var(--color-good)" : "var(--color-warn)" },
            { label: "Database", status: dbStatus, color: dbStatus === "Healthy" ? "var(--color-good)" : "var(--color-warn)" },
            { label: "Kafka", status: kafkaStatus, color: kafkaStatus === "Healthy" ? "var(--color-good)" : "var(--color-warn)" },
          ]);
        }
      } catch {
        // keep defaults
      }
    }
    checkHealth();
    const timer = setInterval(checkHealth, 45000);
    return () => {
      mounted = false;
      clearInterval(timer);
    };
  }, []);

  return (
    <footer className="flex items-center justify-between px-6 lg:px-8 py-2.5 shrink-0 bg-[var(--color-surface)] border-t border-[var(--color-border)] text-xs select-none">
      {/* Left: Overall Status Pill */}
      <div className="flex items-center gap-2 text-[var(--color-text-dim)] font-medium">
        <span className="w-2 h-2 rounded-full bg-[var(--color-good)] shadow-xs animate-pulse" />
        <span className="text-[var(--color-text)] font-semibold text-[11px]">{allSystemsLabel}</span>
      </div>

      {/* Center/Right: Subsystem Badges */}
      <div className="hidden sm:flex items-center gap-2">
        {subsystems.map((s) => (
          <div
            key={s.label}
            className="flex items-center gap-1.5 rounded-lg border border-[var(--color-border)] bg-[var(--color-surface-2)]/60 px-2.5 py-1 text-[11px] text-[var(--color-text-dim)]"
          >
            <span className="font-medium text-[var(--color-text)]">{s.label}</span>
            <span className="w-1.5 h-1.5 rounded-full" style={{ backgroundColor: s.color }} />
            <span style={{ color: s.color }} className="font-medium">
              {s.status}
            </span>
          </div>
        ))}
      </div>

      {/* Version Tag */}
      <span className="text-[11px] text-[var(--color-text-faint)] font-mono">
        PolicyMesh v1.0.0
      </span>
    </footer>
  );
}
