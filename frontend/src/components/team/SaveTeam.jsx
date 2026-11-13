import { useState } from "react";
import { Check, Copy, ExternalLink, Save } from "lucide-react";
import { Link } from "react-router-dom";
import { createTeam } from "../../services/plannerService";
import { GAME_MODES } from "../../util/team";
import { shareLink } from "../../util/share";

const inputClass = "w-full px-3 py-2 rounded-lg border border-border bg-bg-primary text-sm focus:outline-none focus:border-accent-cyan";

/**
 * Name, mode and notes of the draft, plus "Save and share". Saved teams can't change, so the link
 * shown belongs to the exact team that was saved: editing afterwards hides it until saved again.
 */
const SaveTeam = ({ draft, setField, request, canSave }) => {
  const [saving, setSaving] = useState(false);
  const [saved, setSaved] = useState(null); // { key, team }
  const [error, setError] = useState(null);
  const requestKey = request ? JSON.stringify(request) : null;
  const savedTeam = saved && saved.key === requestKey ? saved.team : null;

  const save = async () => {
    setSaving(true);
    setError(null);
    try {
      const team = await createTeam(request);
      setSaved({ key: requestKey, team });
    } catch (err) {
      const status = err.response?.status;
      setError(status === 422
        ? "The team breaks a rule: check the list on the left."
        : err.response?.data?.detail ?? "Couldn't save the team. Try again.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <section className="glass-panel p-5 space-y-3" aria-label="Save and share">
      <label className="block text-sm font-semibold text-text-primary">Team name
        <input type="text" maxLength={60} value={draft.name} onChange={(e) => setField("name", e.target.value)}
          placeholder="e.g. Seele hypercarry" className={`${inputClass} mt-1 font-normal`} />
      </label>
      <label className="block text-sm font-semibold text-text-primary">Game mode
        <select value={draft.gameMode ?? ""} onChange={(e) => setField("gameMode", e.target.value || null)} className={`${inputClass} mt-1 font-normal`}>
          <option value="">Any</option>
          {GAME_MODES.map((mode) => <option key={mode.name} value={mode.name}>{mode.label}</option>)}
        </select>
      </label>
      <label className="block text-sm font-semibold text-text-primary">Notes
        <textarea maxLength={500} rows={3} value={draft.notes} onChange={(e) => setField("notes", e.target.value)}
          placeholder="Rotation, relic sets, who to swap in…" className={`${inputClass} mt-1 font-normal resize-y`} />
      </label>

      {savedTeam ? (
        <ShareBox code={savedTeam.shareCode} />
      ) : (
        <button type="button" onClick={save} disabled={!canSave || saving}
          className="w-full flex items-center justify-center gap-2 px-4 py-2.5 rounded-lg font-semibold bg-text-primary text-bg-card hover:opacity-90 disabled:opacity-40 disabled:cursor-not-allowed">
          <Save size={18} /> {saving ? "Saving…" : "Save and get link"}
        </button>
      )}
      {!request && <p className="text-xs text-text-muted">Add at least one character to save.</p>}
      {error && <p className="text-sm text-accent-red" role="alert">{error}</p>}
    </section>
  );
};

export const ShareBox = ({ code }) => {
  const [copied, setCopied] = useState(false);
  const url = shareLink(code);

  const copy = async () => {
    try {
      await navigator.clipboard.writeText(url);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      setCopied(false); // clipboard blocked: the link is still selectable in the field
    }
  };

  return (
    <div className="rounded-lg border border-[#E3C552] bg-[#FFE68F]/40 p-3 space-y-2">
      <p className="font-hand text-2xl leading-none text-text-primary">ticket {code} printed!</p>
      <div className="flex gap-2">
        <input readOnly value={url} onFocus={(e) => e.target.select()} aria-label="Share link"
          className="flex-1 min-w-0 px-2 py-1.5 rounded-md border border-border bg-bg-card text-sm" />
        <button type="button" onClick={copy}
          className="flex items-center gap-1 px-3 py-1.5 rounded-md bg-text-primary text-bg-card text-sm font-semibold">
          {copied ? <><Check size={16} /> Copied</> : <><Copy size={16} /> Copy</>}
        </button>
      </div>
      <Link to={`/t/${code}`} className="inline-flex items-center gap-1 text-sm text-accent-cyan hover:underline">
        <ExternalLink size={14} /> Open the shared page
      </Link>
    </div>
  );
};

export default SaveTeam;
