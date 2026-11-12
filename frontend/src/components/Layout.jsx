import { memo, useSyncExternalStore } from "react";
import { NavLink } from "react-router-dom";
import { Calculator, Swords, TrainFront, UsersRound } from "lucide-react";
import { isServerWaking, onServerWakingChange } from "../services/apiClient";

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
      </div>
    </header>
    <main className="max-w-7xl mx-auto px-4 py-8">{children}</main>
    <WakingBanner />
  </div>
);

export default memo(Layout);
