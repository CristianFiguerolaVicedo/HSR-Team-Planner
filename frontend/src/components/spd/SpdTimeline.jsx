import { actionTimes, cycleEndAv } from "../../util/spd";

// Theme accents from index.css, one per unit
const COLORS = ["var(--color-accent-cyan)", "var(--color-accent-red)", "var(--color-accent-gold)", "var(--color-accent-purple)"];

/**
 * When each unit acts during the first cycles, drawn on an action value (AV) axis.
 * Dotted lines mark the end of each cycle: an action exactly on a line still counts for that cycle.
 */
const SpdTimeline = ({ units, cycles }) => {
  const total = cycleEndAv(cycles - 1);
  const pct = (av) => `${(av / total) * 100}%`;
  const ends = Array.from({ length: cycles }, (_, c) => cycleEndAv(c));

  return (
    <figure className="glass-panel p-5" aria-labelledby="timeline-title">
      <figcaption id="timeline-title" className="text-lg font-bold mb-1">Turn order</figcaption>
      <p className="text-xs text-text-muted mb-4">Action value from the start of the fight. No action advance or delay.</p>

      <div className="relative ml-28 mb-2 h-4 text-[11px] text-text-muted" aria-hidden="true">
        <span className="absolute -translate-x-1/2" style={{ left: 0 }}>0</span>
        {ends.map((av, c) => (
          <span key={av} className="absolute -translate-x-1/2 whitespace-nowrap" style={{ left: pct(av) }}>
            {av} <span className="hidden sm:inline">(end of {c})</span>
          </span>
        ))}
      </div>

      <ul className="space-y-2">
        {units.map((unit, i) => {
          const times = actionTimes(unit.actionValue, total);
          return (
            <li key={unit.key} className="flex items-center gap-2">
              <span className="w-26 shrink-0 truncate text-sm font-semibold text-right">{unit.label}</span>
              <div className="relative flex-1 h-7 rounded-md bg-bg-secondary" role="img"
                aria-label={`${unit.label} acts at ${times.map((t) => Math.round(t)).join(", ") || "no point before"} AV`}>
                {ends.map((av) => (
                  <span key={av} className="absolute top-0 bottom-0 border-l border-dashed border-text-muted/60" style={{ left: pct(av) }} />
                ))}
                {times.map((t, n) => (
                  <span key={t} title={`Action ${n + 1} at ${t.toFixed(1)} AV`}
                    className="absolute top-1/2 -translate-x-1/2 -translate-y-1/2 w-5 h-5 rounded-full border-2 border-bg-card text-[10px] font-bold text-white flex items-center justify-center shadow-sm"
                    style={{ left: pct(t), background: COLORS[i % COLORS.length] }}>
                    {n + 1}
                  </span>
                ))}
              </div>
            </li>
          );
        })}
      </ul>
    </figure>
  );
};

export default SpdTimeline;
