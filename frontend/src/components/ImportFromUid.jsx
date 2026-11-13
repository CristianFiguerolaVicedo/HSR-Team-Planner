import { useState } from "react";
import { CheckCircle2, ChevronDown, Download, Info } from "lucide-react";
import { useRoster } from "../hooks/useRoster";
import { importFromUid } from "../services/plannerService";
import { compareImport, isValidUid } from "../util/importer";

const UID_KEY = "hsr-planner-uid";

const readUid = () => {
  try { return localStorage.getItem(UID_KEY) ?? ""; } catch { return ""; }
};

const STATUS_LABEL = { new: "new", changed: "Eidolon changed", same: "already in your list" };

/**
 * "Import from UID": reads the in-game profile showcase through the backend (Enka.Network, Mihomo as
 * fallback), shows what would change and adds it to "My characters" only when the user confirms.
 */
const ImportFromUid = () => {
  const { owned, merge } = useRoster();
  const [open, setOpen] = useState(() => Object.keys(owned).length === 0);
  const [uid, setUid] = useState(readUid);
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);
  const [applied, setApplied] = useState(null); // how many characters the last "Add" wrote

  const valid = isValidUid(uid);
  const comparison = result ? compareImport(result.characters, owned) : null;
  const pending = comparison ? Object.keys(comparison.changes).length : 0;

  const load = async (e) => {
    e.preventDefault();
    if (!valid) return;
    const clean = uid.trim();
    setLoading(true);
    setError(null);
    setApplied(null);
    try {
      setResult(await importFromUid(clean));
      try { localStorage.setItem(UID_KEY, clean); } catch { /* not remembered, that's all */ }
    } catch (err) {
      setResult(null);
      setError(err.response?.data?.detail ?? "Couldn't reach the server. Try again.");
    } finally {
      setLoading(false);
    }
  };

  const apply = () => {
    merge(comparison.changes);
    setApplied(pending);
  };

  return (
    <section className="glass-panel mb-4" aria-labelledby="import-title">
      <button type="button" onClick={() => setOpen((v) => !v)} aria-expanded={open}
        className="w-full flex items-center justify-between gap-3 p-4 text-left">
        <span id="import-title" className="flex items-center gap-2 font-bold text-text-primary">
          <Download size={18} className="text-accent-cyan" /> Import from your UID
        </span>
        <ChevronDown size={20} className={`text-text-muted transition-transform ${open ? "rotate-180" : ""}`} />
      </button>

      {open && (
        <div className="px-4 pb-4 border-t border-border pt-4 space-y-4">
          <p className="flex gap-2 text-sm text-text-secondary">
            <Info size={16} className="mt-0.5 shrink-0 text-accent-cyan" />
            Only the characters in your in-game profile showcase (and your support character) are public,
            so put the ones you want to import there first. Nothing is removed from your list.
          </p>

          <form onSubmit={load} className="flex flex-wrap items-center gap-2">
            <label className="sr-only" htmlFor="uid-input">UID</label>
            <input id="uid-input" type="text" inputMode="numeric" autoComplete="off" maxLength={10}
              value={uid} onChange={(e) => setUid(e.target.value.replace(/\D/g, ""))} placeholder="Your UID, e.g. 800333171"
              aria-invalid={uid !== "" && !valid}
              className="w-56 px-3 py-2 rounded-lg border border-border bg-bg-primary text-sm tabular-nums focus:outline-none focus:border-accent-cyan" />
            <button type="submit" disabled={!valid || loading}
              className="px-4 py-2 rounded-lg text-sm font-semibold bg-text-primary text-bg-card hover:opacity-90 disabled:opacity-40 disabled:cursor-not-allowed">
              {loading ? "Reading profile…" : "Read profile"}
            </button>
            {uid !== "" && !valid && <span className="text-xs text-text-muted">A UID has 9 or 10 digits.</span>}
          </form>

          {error && <p className="text-sm text-accent-red" role="alert">{error}</p>}

          {result && comparison && (
            <div className="space-y-3">
              <p className="text-sm text-text-secondary">
                <span className="font-semibold text-text-primary">{result.nickname ?? "Player"}</span>
                {result.level != null && <> · Trailblaze Level {result.level}</>}
                {" "}· {result.characters.length} character{result.characters.length === 1 ? "" : "s"} in the showcase
                <span className="text-text-muted"> (via {result.source === "ENKA" ? "Enka.Network" : "Mihomo"})</span>
              </p>

              {result.characters.length === 0 ? (
                <p className="font-hand text-2xl text-text-secondary">the showcase is empty: add characters to it in the game</p>
              ) : (
                <ul className="grid gap-2 sm:grid-cols-2 lg:grid-cols-4">
                  {comparison.rows.map(({ character, eidolon, current, status }) => (
                    <li key={character.id} className="flex items-center gap-2 p-2 rounded-lg border border-border bg-bg-card">
                      {character.miniIconUrl
                        ? <img src={character.miniIconUrl} alt="" loading="lazy" className="w-10 h-10 rounded-full bg-bg-elevated object-cover" />
                        : <span className="w-10 h-10 rounded-full bg-bg-elevated" />}
                      <span className="min-w-0">
                        <span className="block text-sm font-semibold text-text-primary truncate">{character.name}</span>
                        <span className={`block text-xs ${status === "same" ? "text-text-muted" : "text-accent-cyan font-semibold"}`}>
                          {status === "changed" ? `E${current} → E${eidolon}` : `E${eidolon}`} · {STATUS_LABEL[status]}
                        </span>
                      </span>
                    </li>
                  ))}
                </ul>
              )}

              {result.unknownAvatarIds.length > 0 && (
                <p className="text-xs text-text-muted">
                  {result.unknownAvatarIds.length} character{result.unknownAvatarIds.length === 1 ? " isn't" : "s aren't"} in
                  the catalog yet (probably just released); they'll appear after the next sync.
                </p>
              )}

              {applied !== null && pending === 0 ? (
                <p className="flex items-center gap-2 text-sm font-semibold text-text-primary" role="status">
                  <CheckCircle2 size={18} className="text-accent-cyan" />
                  {applied > 0 ? `Done: ${applied} character${applied === 1 ? "" : "s"} updated.` : "Your list already matches the showcase."}
                </p>
              ) : pending > 0 ? (
                <button type="button" onClick={apply}
                  className="px-4 py-2 rounded-lg text-sm font-semibold bg-text-primary text-bg-card hover:opacity-90">
                  Add to my characters ({comparison.newCount} new{comparison.changedCount > 0 ? `, ${comparison.changedCount} Eidolon update${comparison.changedCount === 1 ? "" : "s"}` : ""})
                </button>
              ) : result.characters.length > 0 && (
                <p className="text-sm text-text-muted">Everything in the showcase is already in your list.</p>
              )}

              {result.refreshAfter && (
                <p className="text-xs text-text-muted">
                  Changed your showcase? New data is available after {new Date(result.refreshAfter).toLocaleTimeString()}.
                </p>
              )}
            </div>
          )}
        </div>
      )}
    </section>
  );
};

export default ImportFromUid;
