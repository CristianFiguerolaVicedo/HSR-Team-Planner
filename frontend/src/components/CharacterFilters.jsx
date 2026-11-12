import { useState } from "react";
import { Check, Route, Search, Shield, Star, Zap } from "lucide-react";
import { ELEMENTS, PATHS, ROLES } from "../util/game";
import { EMPTY_FILTERS, activeFilterCount, toggleIn } from "../util/filters";

// Same chip look as HSRWiki's CharFilters (highlighter yellow when active)
const chipActiveClass = "bg-[#FFE68F] border border-[#E3C552] text-text-primary font-semibold";
const chipInactiveClass = "bg-bg-card border border-border text-text-secondary hover:bg-bg-elevated hover:border-accent-cyan/30 hover:text-text-primary";
const chipClass = (active) =>
  `px-3 py-1.5 rounded-lg transition-all duration-200 flex items-center gap-2 text-sm ${active ? chipActiveClass : chipInactiveClass}`;

const RARITIES = [5, 4];

const Section = ({ icon, iconClass, title, children }) => {
  const Icon = icon;
  return (
  <div>
    <h3 className="text-sm font-semibold text-text-primary mb-2 flex items-center gap-2">
      <Icon size={16} className={iconClass} /> {title}
    </h3>
    <div className="flex flex-wrap gap-2">{children}</div>
  </div>
  );
};

/** Ownership switch with counts, same as HSRWiki's OwnershipFilter. */
export const OwnershipFilter = ({ value, onChange, ownedCount, total }) => (
  <div className="inline-flex rounded-lg border border-border bg-bg-card p-0.5" role="group" aria-label="Collection filter">
    {[["all", `All (${total})`], ["owned", `Owned (${ownedCount})`], ["missing", `Missing (${total - ownedCount})`]].map(([key, text]) => (
      <button
        key={key}
        type="button"
        onClick={() => onChange(key)}
        aria-pressed={value === key}
        className={`px-3 py-1.5 rounded-md text-sm cursor-pointer transition-colors ${value === key
          ? "bg-text-primary text-bg-primary font-semibold"
          : "text-text-secondary hover:text-text-primary"}`}
      >
        {text}
      </button>
    ))}
  </div>
);

/** Controlled: the page owns the filters, so the same state drives the grid and the bulk actions. */
const CharacterFilters = ({ filters, onChange, ownedCount, total, shown }) => {
  const [open, setOpen] = useState(false);
  const set = (patch) => onChange({ ...filters, ...patch });
  const count = activeFilterCount(filters);

  return (
    <div className="w-full glass-panel mb-6">
      <div className="flex flex-wrap items-center gap-3 p-4">
        <label className="relative flex-1 min-w-48">
          <span className="sr-only">Search characters by name</span>
          <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-text-muted" aria-hidden="true" />
          <input
            type="search"
            value={filters.search}
            onChange={(e) => set({ search: e.target.value })}
            placeholder="Search by name..."
            className="w-full pl-9 pr-3 py-2 rounded-lg border border-border bg-bg-primary text-sm focus:outline-none focus:border-accent-cyan"
          />
        </label>
        <OwnershipFilter value={filters.ownership} onChange={(ownership) => set({ ownership })} ownedCount={ownedCount} total={total} />
        <button
          type="button"
          onClick={() => setOpen((v) => !v)}
          aria-expanded={open}
          className="flex items-center gap-2 px-3 py-2 rounded-lg text-sm text-text-secondary hover:text-text-primary hover:bg-bg-elevated transition-colors"
        >
          Filters
          {count > 0 && (
            <span className="px-2 py-0.5 bg-accent-cyan/20 text-accent-cyan text-xs rounded-full border border-accent-cyan/30">{count} active</span>
          )}
          <svg className={`w-4 h-4 transition-transform ${open ? "rotate-180" : ""}`} fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
          </svg>
        </button>
        {count > 0 && (
          <button type="button" onClick={() => onChange(EMPTY_FILTERS)}
            className="px-3 py-1 text-sm text-text-secondary hover:text-text-primary hover:bg-bg-elevated rounded-lg transition-colors">
            Clear All
          </button>
        )}
        <span className="text-sm text-text-muted ml-auto" aria-live="polite">
          {shown === total ? `${total} characters` : `${shown} of ${total}`}
        </span>
      </div>

      {open && (
        <div className="p-4 border-t border-border grid gap-5 md:grid-cols-2">
          <Section icon={Zap} iconClass="text-accent-cyan" title="Element">
            {ELEMENTS.map((el) => {
              const active = filters.elements.includes(el.name);
              return (
                <button key={el.name} type="button" aria-pressed={active} className={chipClass(active)}
                  onClick={() => set({ elements: toggleIn(filters.elements, el.name) })}>
                  {active ? <Check size={16} className="text-accent-cyan" /> : <img src={el.icon} alt="" className="w-4 h-4" />}
                  {el.label}
                </button>
              );
            })}
          </Section>

          <Section icon={Route} iconClass="text-accent-gold" title="Path">
            {PATHS.map((path) => {
              const active = filters.paths.includes(path.name);
              return (
                <button key={path.name} type="button" aria-pressed={active} className={chipClass(active)}
                  onClick={() => set({ paths: toggleIn(filters.paths, path.name) })}>
                  {active ? <Check size={16} className="text-accent-cyan" /> : <img src={path.icon} alt="" className="w-4 h-4 invert opacity-70" />}
                  {path.label}
                </button>
              );
            })}
          </Section>

          <Section icon={Shield} iconClass="text-accent-purple" title="Role">
            {ROLES.map((role) => {
              const active = filters.roles.includes(role.name);
              return (
                <button key={role.name} type="button" aria-pressed={active} className={chipClass(active)}
                  onClick={() => set({ roles: toggleIn(filters.roles, role.name) })}>
                  {active && <Check size={16} className="text-accent-cyan" />}
                  {role.label}
                </button>
              );
            })}
          </Section>

          <Section icon={Star} iconClass="text-accent-gold fill-accent-gold" title="Rarity">
            {RARITIES.map((rarity) => {
              const active = filters.rarities.includes(rarity);
              return (
                <button key={rarity} type="button" aria-pressed={active} className={chipClass(active)}
                  onClick={() => set({ rarities: toggleIn(filters.rarities, rarity) })}>
                  {active && <Check size={16} className="text-accent-gold" />}
                  {rarity}★
                </button>
              );
            })}
          </Section>
        </div>
      )}
    </div>
  );
};

export default CharacterFilters;
