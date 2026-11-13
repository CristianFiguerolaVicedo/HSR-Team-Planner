import { memo, useState, useSyncExternalStore } from "react";
import { NavLink } from "react-router-dom";
import { Calculator, Swords, TrainFront, UsersRound, X } from "lucide-react";
import { isServerWaking, onServerWakingChange } from "../services/apiClient";
import AccountMenu from "./AccountMenu";

const LINKS = [
  { to: "/", label: "Characters", icon: TrainFront, end: true },
  { to: "/roster", label: "My characters", icon: UsersRound },
  { to: "/builder", label: "Team builder", icon: Swords },
  { to: "/spd", label: "SPD calculator", icon: Calculator },
];

/** Same banner as HSRWiki: shown while requests are slow because the API is starting. */
const WakingBanner = () => {
  const waking = useSyncExternalStore(onServerWakingChange, isServerWaking);
  if (!waking) return null;
  return (
    <div
      role="status"
      className="fixed bottom-4 left-1/2 -translate-x-1/2 z-50 w-[calc(100%-2rem)] max-w-md glass-panel px-4 py-3 flex items-center gap-3 shadow-lg"
    >
      <span className="w-4 h-4 shrink-0 rounded-full border-2 border-accent-red border-t-transparent animate-spin" aria-hidden="true" />
      <p className="text-sm leading-snug">
        <span className="font-semibold">Waking up the server…</span>{" "}
        <span className="text-text-secondary">The first load can take a moment while the API starts.</span>
      </p>
    </div>
  );
};

/** Shown when Discord/Google sent the user back with an error (the backend redirects to /?login=error). */
const LoginErrorBanner = () => {
  const [visible, setVisible] = useState(() => new URLSearchParams(window.location.search).get("login") === "error");
  if (!visible) return null;
  const close = () => {
    setVisible(false);
    window.history.replaceState(null, "", window.location.pathname); // drop ?login=error
  };
  return (
    <div role="alert" className="max-w-7xl mx-auto mt-4 px-4">
      <div className="glass-panel p-3 flex items-center gap-3 text-sm">
        <span className="flex-1"><span className="font-semibold text-accent-red">Sign in didn't work.</span> Try again, or keep using the planner without an account.</span>
        <button type="button" onClick={close} aria-label="Dismiss" className="p-1 rounded text-text-muted hover:text-text-primary"><X size={16} /></button>
      </div>
    </div>
  );
};

const Layout = ({ children }) => (
  <div className="min-h-screen">
    <header className="sticky top-0 z-40 border-b border-border bg-bg-primary/90 backdrop-blur">
      <div className="max-w-7xl mx-auto px-4 py-3 flex flex-wrap items-center gap-x-6 gap-y-2">
        <NavLink to="/" className="flex items-baseline gap-2">
          <span className="text-xl font-extrabold tracking-tight text-text-primary">HSR Team Planner</span>
          <span className="font-hand text-xl text-accent-cyan hidden sm:inline">dispatch office</span>
        </NavLink>
        <nav className="flex flex-wrap gap-1" aria-label="Main">
          {LINKS.map(({ to, label, icon, end }) => {
            const Icon = icon;
            return (
            <NavLink
              key={to}
              to={to}
              end={end}
              className={({ isActive }) => `flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-sm transition-colors ${isActive
                ? "bg-text-primary text-bg-primary font-semibold"
                : "text-text-secondary hover:text-text-primary hover:bg-bg-elevated"}`}
            >
              <Icon size={16} aria-hidden="true" />
              {label}
            </NavLink>
            );
          })}
        </nav>
        <div className="ml-auto"><AccountMenu /></div>
      </div>
    </header>
    <LoginErrorBanner />
    <main className="max-w-7xl mx-auto px-4 py-8">{children}</main>
    <WakingBanner />
  </div>
);

export default memo(Layout);
