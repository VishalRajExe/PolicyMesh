import { createContext, useContext, useState } from "react";

const LayoutContext = createContext({
  mobileNavOpen: false,
  openMobileNav: () => {},
  closeMobileNav: () => {},
  toggleMobileNav: () => {},
});

export function LayoutProvider({ children }) {
  const [mobileNavOpen, setMobileNavOpen] = useState(false);

  const openMobileNav = () => setMobileNavOpen(true);
  const closeMobileNav = () => setMobileNavOpen(false);
  const toggleMobileNav = () => setMobileNavOpen((prev) => !prev);

  return (
    <LayoutContext.Provider
      value={{
        mobileNavOpen,
        openMobileNav,
        closeMobileNav,
        toggleMobileNav,
      }}
    >
      {children}
    </LayoutContext.Provider>
  );
}

export function useLayout() {
  return useContext(LayoutContext);
}
