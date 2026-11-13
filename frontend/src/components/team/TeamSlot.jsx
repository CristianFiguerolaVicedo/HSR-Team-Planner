import { AlertTriangle, Gauge, Plus, RefreshCw, Star, X, XCircle } from "lucide-react";
import { ELEMENT, PATH, ROLE, tapeClass, tiltFor } from "../../util/game";
import { actionsLabel, formatBreakpoint, formatMissing } from "../../util/spd";

const selectClass = "w-full px-2 py-1.5 rounded-lg border border-border bg-bg-primary text-sm focus:outline-none focus:border-accent-cyan disabled:opacity-50";

/**
 * One of the 4 places in a team. Editable in the builder; readOnly on a shared team.
 * spdAnalysis comes from the backend (POST /spd/team or GET /teams/{code}).
 */
const TeamSlot = ({
  position, slot, char, lightcone, spdAnalysis, issues = [], readOnly = false,
  onAdd, onChangeCharacter, onChooseLightcone, onUpdate, onClear,
}) => {
  if (!slot || !char) {
    if (readOnly) return <div className="rounded-xl border-2 border-dashed border-border min-h-80" aria-label={`Slot ${position + 1} empty`} />;
    return (
      <button type="button" onClick={onAdd}
        className="w-full min-h-80 rounded-xl border-2 border-dashed border-border bg-bg-card/50 flex flex-col items-center justify-center gap-2 text-text-muted hover:text-text-primary hover:border-accent-cyan/50 transition-colors">
        <Plus size={28} />
        <span className="font-hand text-2xl">add character</span>
        <span className="text-xs">Slot {position + 1}</span>
      </button>
    );
  }

  const element = ELEMENT[char.element];
  const path = PATH[char.path];

  return (
    <article className={`photo-card ${tapeClass(char.element)}`} aria-label={`Slot ${position + 1}: ${char.name}`}
      style={{ "--tilt": `${tiltFor(char.id + position, 0.4)}deg`, "--tape-tilt": `${tiltFor(char.id, 1.2)}deg` }}>
      <span className="washi" aria-hidden="true" />

      <div className="photo-card__picture h-40 relative">
        {char.iconUrl && <img src={char.iconUrl} alt="" className="h-full w-full object-cover object-top" />}
        <span className="absolute top-2 left-2 px-2 py-0.5 rounded-md bg-text-primary text-bg-primary text-xs font-bold tabular-nums">E{slot.eidolon}</span>
        {!readOnly && (
          <div className="absolute top-2 right-2 flex gap-1">
            <button type="button" onClick={onChangeCharacter} aria-label={`Change ${char.name}`} title="Change character"
              className="p-1.5 rounded-md bg-bg-card/90 text-text-secondary hover:text-text-primary"><RefreshCw size={14} /></button>
            <button type="button" onClick={onClear} aria-label={`Remove ${char.name}`} title="Remove"
              className="p-1.5 rounded-md bg-bg-card/90 text-text-secondary hover:text-accent-red"><X size={14} /></button>
          </div>
        )}
      </div>

      <div className="pt-3 px-1 space-y-2.5">
        <div>
          <div className="flex items-start justify-between gap-2">
            <h3 className="font-semibold text-[1.05rem] leading-tight text-text-primary">{char.name}</h3>
            <div className="flex shrink-0 pt-0.5" aria-label={`${char.rarity} stars`}>
              {Array.from({ length: char.rarity }).map((_, i) => (
                <Star key={i} size={11} className={char.rarity === 5 ? "text-accent-gold fill-accent-gold" : "text-accent-purple fill-accent-purple"} />
              ))}
            </div>
          </div>
          <div className="mt-1 flex items-center gap-1.5 text-text-secondary">
            {element && <img src={element.icon} alt="" className="w-5 h-5" title={element.label} />}
            {path && <img src={path.icon} alt="" className="w-5 h-5 invert opacity-70" title={path.label} />}
            <span className="font-hand text-xl leading-none">{ROLE[char.role]?.label ?? char.role}</span>
          </div>
        </div>

        {readOnly ? (
          <p className="text-sm text-text-secondary">
            {lightcone ? <><span className="font-semibold text-text-primary">{lightcone.name}</span> · S{slot.superimposition ?? 1}</> : "No light cone"}
          </p>
        ) : (
          <>
            <div className="grid grid-cols-2 gap-2">
              <label className="text-xs text-text-muted">Eidolon
                <select value={slot.eidolon} onChange={(e) => onUpdate({ eidolon: Number(e.target.value) })} className={`${selectClass} mt-0.5`}>
                  {[0, 1, 2, 3, 4, 5, 6].map((e) => <option key={e} value={e}>E{e}</option>)}
                </select>
              </label>
              <label className="text-xs text-text-muted">Superimp.
                <select value={slot.superimposition ?? 1} disabled={!slot.lightConeId}
                  onChange={(e) => onUpdate({ superimposition: Number(e.target.value) })} className={`${selectClass} mt-0.5`}>
                  {[1, 2, 3, 4, 5].map((s) => <option key={s} value={s}>S{s}</option>)}
                </select>
              </label>
            </div>
            <button type="button" onClick={onChooseLightcone}
              className="w-full flex items-center gap-2 px-2 py-1.5 rounded-lg border border-border bg-bg-primary text-left text-sm hover:border-accent-cyan/50">
              {lightcone?.iconUrl && <img src={lightcone.iconUrl} alt="" className="w-7 h-7 object-contain" />}
              <span className={`truncate ${lightcone ? "text-text-primary font-semibold" : "text-text-muted"}`}>
                {lightcone ? lightcone.name : "Choose light cone"}
              </span>
            </button>
          </>
        )}

        <SpdBlock slot={slot} char={char} analysis={spdAnalysis} readOnly={readOnly} onUpdate={onUpdate} />

        {issues.length > 0 && (
          <ul className="space-y-1">
            {issues.map((issue) => (
              <li key={issue.code} className={`flex gap-1.5 text-xs ${issue.severity === "ERROR" ? "text-accent-red" : "text-text-secondary"}`}>
                {issue.severity === "ERROR" ? <XCircle size={14} className="shrink-0" /> : <AlertTriangle size={14} className="shrink-0 text-accent-gold" />}
                {issue.message}
              </li>
            ))}
          </ul>
        )}
      </div>
    </article>
  );
};

const SpdBlock = ({ slot, char, analysis, readOnly, onUpdate }) => {
  const cycleZero = analysis?.cycles?.[0];
  return (
    <div className="rounded-lg bg-bg-secondary px-2.5 py-2">
      {readOnly ? (
        <p className="flex items-center gap-1.5 text-sm"><Gauge size={14} className="text-text-muted" />
          <span className="font-semibold tabular-nums">{slot.spd ?? "—"}</span> <span className="text-text-muted">SPD</span></p>
      ) : (
        <label className="flex items-center gap-2 text-sm">
          <Gauge size={14} className="text-text-muted shrink-0" />
          <span className="text-text-muted">SPD</span>
          <input type="number" inputMode="decimal" min="1" max="1000" step="0.1" placeholder={String(char.baseSpd)}
            value={slot.spd ?? ""}
            onChange={(e) => onUpdate({ spd: e.target.value === "" ? null : Number(e.target.value) })}
            className="w-full min-w-0 px-2 py-1 rounded-md border border-border bg-bg-primary text-sm tabular-nums focus:outline-none focus:border-accent-cyan" />
        </label>
      )}
      {cycleZero ? (
        <p className="mt-1 text-xs text-text-secondary">
          <span className="font-semibold text-text-primary">{actionsLabel(cycleZero.actions)}</span> in cycle 0 ·{" "}
          {formatBreakpoint(cycleZero.nextBreakpoint)} for {cycleZero.actions + 1}{" "}
          <span className="text-accent-cyan font-semibold">({formatMissing(cycleZero.spdMissing)})</span>
        </p>
      ) : (
        <p className="mt-1 text-xs text-text-muted">{slot.spd >= 1 && slot.spd <= 1000 ? "Calculating…" : `Base ${char.baseSpd}. Enter your final SPD (1–1000).`}</p>
      )}
    </div>
  );
};

export default TeamSlot;
