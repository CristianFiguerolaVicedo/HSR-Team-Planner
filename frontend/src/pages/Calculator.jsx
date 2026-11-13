import { useMemo, useState } from "react";
import { Gauge, Plus, X } from "lucide-react";
import { useCharacters } from "../hooks/useCharacters";
import { useRoster } from "../hooks/useRoster";
import { useDebouncedQuery } from "../hooks/useDebouncedQuery";
import { getTeamSpd } from "../services/plannerService";
import SpdTimeline from "../components/spd/SpdTimeline";
import { actionsLabel, breakpointTable, formatBreakpoint, formatMissing } from "../util/spd";

const MAX_UNITS = 4;
const CYCLE_OPTIONS = [1, 2, 3, 4, 5];
const inputClass = "px-2 py-1.5 rounded-lg border border-border bg-bg-primary text-sm focus:outline-none focus:border-accent-cyan";

// Module-level so useDebouncedQuery gets the same function every render
const fetchSpd = ({ speeds, cycles }) => getTeamSpd(speeds, cycles);

let nextKey = 1;
const newUnit = (spd = 134) => ({ key: nextKey++, characterId: "", spd });

const validSpd = (spd) => Number.isFinite(spd) && spd >= 1 && spd <= 1000;

/**
 * SPD calculator: up to 4 units, actions per cycle and the SPD missing for the next breakpoint.
 * The numbers come from the backend (POST /spd/team, the JUnit-tested SpdCalculator).
 */
const Calculator = () => {
  const { characters } = useCharacters();
  const { owned } = useRoster();
  const [units, setUnits] = useState(() => [newUnit()]);
  const [cycles, setCycles] = useState(3);

  const charactersById = useMemo(() => Object.fromEntries(characters.map((c) => [c.id, c])), [characters]);
  const [mine, others] = useMemo(() => [
    characters.filter((c) => String(c.id) in owned),
    characters.filter((c) => !(String(c.id) in owned)),
  ], [characters, owned]);

  const ready = units.filter((u) => validSpd(u.spd));
  const payload = ready.length ? { speeds: ready.map((u) => u.spd), cycles } : null;
  const { data, error, loading } = useDebouncedQuery(fetchSpd, payload);

  // Pair each answer with its unit, only while it still matches what the user typed
  const analysisByKey = {};
  if (data) {
    ready.forEach((unit, i) => {
      if (data[i]?.spd === unit.spd && data[i].cycles.length === cycles) analysisByKey[unit.key] = data[i];
    });
  }

  const update = (key, patch) => setUnits((list) => list.map((u) => (u.key === key ? { ...u, ...patch } : u)));
  const chooseCharacter = (key, value) => {
    const char = charactersById[value];
    update(key, char ? { characterId: value, spd: char.baseSpd } : { characterId: "" });
  };
  const labelOf = (unit, index) => charactersById[unit.characterId]?.name ?? `Unit ${index + 1}`;

  const timelineUnits = units
    .map((u, i) => ({ key: u.key, label: labelOf(u, i), actionValue: analysisByKey[u.key]?.actionValue }))
    .filter((u) => u.actionValue);

  return (
    <div className="text-text-primary">
      <div className="mb-6">
        <h1 className="text-5xl md:text-6xl font-extrabold tracking-tight">SPD calculator</h1>
        <p className="font-hand text-2xl text-text-secondary mt-1">how many turns before the next cycle?</p>
      </div>

      <div className="glass-panel p-4 mb-6 flex flex-wrap items-center gap-3">
        <label className="flex items-center gap-2 text-sm font-semibold">
          Cycles to show
          <select value={cycles} onChange={(e) => setCycles(Number(e.target.value))} className={inputClass}>
            {CYCLE_OPTIONS.map((c) => <option key={c} value={c}>{c} (0–{c - 1})</option>)}
          </select>
        </label>
        <button type="button" onClick={() => setUnits((list) => [...list, newUnit()])} disabled={units.length >= MAX_UNITS}
          className="flex items-center gap-1.5 px-3 py-2 rounded-lg text-sm font-semibold bg-text-primary text-bg-card hover:opacity-90 disabled:opacity-40 disabled:cursor-not-allowed">
          <Plus size={16} /> Add unit ({units.length}/{MAX_UNITS})
        </button>
        {loading && <span className="text-sm text-text-muted" aria-live="polite">Calculating…</span>}
        {error && <span className="text-sm text-accent-red" role="alert">{error.response?.data?.detail ?? "Couldn't reach the API."}</span>}
      </div>

      <div className="grid gap-5 md:grid-cols-2 mb-6">
        {units.map((unit, index) => (
          <UnitCard key={unit.key} unit={unit} label={labelOf(unit, index)} analysis={analysisByKey[unit.key]}
            mine={mine} others={others} canRemove={units.length > 1}
            onCharacter={(value) => chooseCharacter(unit.key, value)}
            onSpd={(spd) => update(unit.key, { spd })}
            onRemove={() => setUnits((list) => list.filter((u) => u.key !== unit.key))} />
        ))}
      </div>

      {timelineUnits.length > 0 && <div className="mb-6"><SpdTimeline units={timelineUnits} cycles={cycles} /></div>}

      <BreakpointTable cycles={cycles} />
    </div>
  );
};

const UnitCard = ({ unit, label, analysis, mine, others, canRemove, onCharacter, onSpd, onRemove }) => (
  <section className="glass-panel p-4 space-y-3" aria-label={label}>
    <div className="flex items-center gap-2">
      <select value={unit.characterId} onChange={(e) => onCharacter(e.target.value)} aria-label="Character"
        className={`${inputClass} flex-1 min-w-0`}>
        <option value="">Custom unit</option>
        {mine.length > 0 && (
          <optgroup label="My characters">
            {mine.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
          </optgroup>
        )}
        <optgroup label={mine.length > 0 ? "Other characters" : "Characters"}>
          {others.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
        </optgroup>
      </select>
      {canRemove && (
        <button type="button" onClick={onRemove} aria-label={`Remove ${label}`}
          className="p-1.5 rounded-lg text-text-muted hover:text-accent-red hover:bg-accent-red-muted"><X size={18} /></button>
      )}
    </div>

    <label className="flex items-center gap-2 text-sm">
      <Gauge size={16} className="text-text-muted" />
      <span className="font-semibold">SPD</span>
      <input type="number" inputMode="decimal" min="1" max="1000" step="0.1" value={Number.isFinite(unit.spd) ? unit.spd : ""}
        onChange={(e) => onSpd(e.target.value === "" ? NaN : Number(e.target.value))}
        className={`${inputClass} w-28 tabular-nums`} />
      {analysis && <span className="text-text-muted tabular-nums">AV {analysis.actionValue.toFixed(2)}</span>}
    </label>

    {!validSpd(unit.spd) ? (
      <p className="text-sm text-text-muted">Enter a SPD between 1 and 1000.</p>
    ) : analysis ? (
      <table className="w-full text-sm">
        <thead>
          <tr className="text-left text-text-muted">
            <th className="py-1 font-semibold">Cycle</th>
            <th className="py-1 font-semibold">Actions</th>
            <th className="py-1 font-semibold">Total</th>
            <th className="py-1 font-semibold">Next breakpoint</th>
          </tr>
        </thead>
        <tbody>
          {analysis.cycles.map((c) => (
            <tr key={c.cycle} className="border-t border-border">
              <td className="py-1.5">{c.cycle}</td>
              <td className="py-1.5 font-semibold">{actionsLabel(c.actions)}</td>
              <td className="py-1.5 tabular-nums">{c.totalActions}</td>
              <td className="py-1.5 tabular-nums">
                {formatBreakpoint(c.nextBreakpoint)} <span className="text-accent-cyan font-semibold">({formatMissing(c.spdMissing)})</span>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    ) : (
      <div className="h-24 rounded-lg bg-bg-elevated animate-pulse" aria-busy="true" />
    )}
  </section>
);

/** Usual breakpoints between 100 and 200 SPD, for quick reference while building relics. */
const BreakpointTable = ({ cycles }) => {
  const rows = breakpointTable(cycles);
  return (
    <section className="glass-panel p-5" aria-labelledby="bp-title">
      <h2 id="bp-title" className="text-lg font-bold mb-1">Breakpoints between 100 and 200 SPD</h2>
      <p className="text-xs text-text-muted mb-3">Total actions by the end of each cycle, counting from the start of the fight.</p>
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {Array.from({ length: cycles }, (_, cycle) => (
          <div key={cycle}>
            <h3 className="font-hand text-2xl text-accent-cyan leading-none mb-1">by end of cycle {cycle}</h3>
            <ul className="text-sm">
              {rows.filter((r) => r.cycle === cycle).map((r) => (
                <li key={r.actions} className="flex justify-between border-t border-border py-1">
                  <span>{actionsLabel(r.actions)}</span>
                  <span className="font-semibold tabular-nums">{formatBreakpoint(r.spd)}</span>
                </li>
              ))}
            </ul>
          </div>
        ))}
      </div>
    </section>
  );
};

export default Calculator;
