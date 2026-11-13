import { AlertTriangle, CheckCircle2, Info, Link2, XCircle } from "lucide-react";
import { ROLES } from "../../util/game";

const SEVERITY = {
  ERROR: { icon: XCircle, className: "text-accent-red" },
  WARNING: { icon: AlertTriangle, className: "text-accent-gold" },
  INFO: { icon: Info, className: "text-accent-cyan" },
};

/** Rules, roles and synergies of a team, as POST /teams/validate (or GET /teams/{code}) returns them. */
const ValidationPanel = ({ validation, loading = false, error = null, empty = false }) => {
  if (empty) {
    return (
      <section className="glass-panel p-5" aria-label="Team check">
        <p className="font-hand text-2xl text-text-secondary">add a character to start checking the team</p>
      </section>
    );
  }

  return (
    <section className={`glass-panel p-5 space-y-5 transition-opacity ${loading ? "opacity-60" : ""}`} aria-label="Team check" aria-busy={loading}>
      {error && (
        <p className="text-sm text-accent-red" role="alert">
          Couldn't check the team: {error.response?.data?.detail ?? error.message}
        </p>
      )}

      {validation && (
        <>
          <Status validation={validation} />

          {validation.issues.length > 0 && (
            <ul className="space-y-1.5">
              {validation.issues.map((issue, i) => {
                const { icon: SeverityIcon, className } = SEVERITY[issue.severity] ?? SEVERITY.INFO;
                return (
                  <li key={`${issue.code}-${issue.position ?? "team"}-${i}`} className="flex gap-2 text-sm text-text-primary">
                    <SeverityIcon size={16} className={`shrink-0 mt-0.5 ${className}`} />
                    <span>{issue.position != null && <span className="text-text-muted">Slot {issue.position + 1}: </span>}{issue.message}</span>
                  </li>
                );
              })}
            </ul>
          )}

          <div>
            <h3 className="text-sm font-semibold text-text-primary mb-2">Roles</h3>
            <div className="grid grid-cols-4 gap-2">
              {ROLES.map((role) => (
                <div key={role.name} className="rounded-lg bg-bg-secondary px-2 py-1.5 text-center">
                  <div className="text-xl font-extrabold tabular-nums text-text-primary">{validation.roles?.[role.name] ?? 0}</div>
                  <div className="text-[11px] text-text-muted leading-tight">{role.label}</div>
                </div>
              ))}
            </div>
          </div>

          <div>
            <h3 className="flex items-center justify-between text-sm font-semibold text-text-primary mb-2">
              <span className="flex items-center gap-1.5"><Link2 size={16} className="text-accent-cyan" /> Synergies</span>
              <span className="tabular-nums">score {validation.synergyScore}</span>
            </h3>
            {validation.synergyPairs.length === 0 ? (
              <p className="text-sm text-text-muted">No known synergies between these characters yet.</p>
            ) : (
              <ul className="space-y-1">
                {validation.synergyPairs.map((pair) => (
                  <li key={`${pair.characterId}-${pair.otherCharacterId}`} className="flex items-center justify-between gap-3 text-sm">
                    <span className="truncate">{pair.characterName} <span className="text-text-muted">+</span> {pair.otherCharacterName}</span>
                    <span className="tabular-nums text-accent-gold font-semibold">+{pair.score}</span>
                  </li>
                ))}
              </ul>
            )}
          </div>
        </>
      )}
    </section>
  );
};

const Status = ({ validation }) => {
  const errors = validation.issues.filter((i) => i.severity === "ERROR").length;
  const warnings = validation.issues.filter((i) => i.severity === "WARNING").length;
  if (!validation.valid) {
    return <p className="flex items-center gap-2 font-semibold text-accent-red"><XCircle size={20} /> {errors} rule{errors === 1 ? "" : "s"} broken: fix before saving</p>;
  }
  return (
    <p className="flex items-center gap-2 font-semibold text-text-primary">
      <CheckCircle2 size={20} className="text-accent-cyan" />
      {warnings === 0 ? "Looks good" : `Valid, with ${warnings} warning${warnings === 1 ? "" : "s"}`}
    </p>
  );
};

export default ValidationPanel;
