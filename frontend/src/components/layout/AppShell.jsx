import { useState } from "react";
import Sidebar from "./Sidebar";
import StatusBar from "./StatusBar";
import { LayoutProvider, useLayout } from "../../context/LayoutContext";

function AppShellContent({ children }) {
  const [collapsed, setCollapsed] = useState(false);
  const { mobileNavOpen, closeMobileNav } = useLayout();

  return (
    <div className="h-screen w-screen flex bg-[var(--color-bg)] overflow-hidden">
      {/* Sidebar (Desktop permanent + Mobile slide-in drawer) */}
      <Sidebar
        collapsed={collapsed}
        onToggle={() => setCollapsed((c) => !c)}
        mobileOpen={mobileNavOpen}
        onMobileClose={closeMobileNav}
      />

      {/* Main Content Area + Footer */}
      <div className="flex-1 flex flex-col min-w-0 h-full overflow-hidden">
        <main className="flex-1 overflow-y-auto min-w-0">
          {children}
        </main>
        <StatusBar />
      </div>
    </div>
  );
}

export default function AppShell({ children }) {
  return (
    <LayoutProvider>
      <AppShellContent>{children}</AppShellContent>
    </LayoutProvider>
  );
}
