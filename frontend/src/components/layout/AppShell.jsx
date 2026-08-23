import { useState } from "react";
import Sidebar from "./Sidebar";

export default function AppShell({ children }) {
  const [collapsed, setCollapsed] = useState(false);

  return (
    <div className="h-screen w-screen flex bg-[var(--color-bg)] overflow-hidden">
      <Sidebar collapsed={collapsed} onToggle={() => setCollapsed((c) => !c)} />
      <div className="flex-1 flex flex-col min-w-0">
        <div className="flex-1 overflow-y-auto">{children}</div>
      </div>
    </div>
  );
}
