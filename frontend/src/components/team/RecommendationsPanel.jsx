import { useState } from "react";
import { Link } from "react-router-dom";
import { ChevronDown, Lightbulb, Sparkles, Wand2 } from "lucide-react";
import { getRecommendations } from "../../services/plannerService";
import { roleLabel } from "../../util/game";
import { GAME_MODES, TEAM_SIZE } from "../../util/team";

const MIN_OWNED = 4;

/**
 * "Suggested teams": asks the backend's rule-based recommender (POST /recommendations) for the best
 * teams it can make from your characters, optionally around the ones already in the builder.
 */
const RecommendationsPanel = ({ owned, gameMode, onGameModeChange, memberIds, onUse }) => {
  const [open, setOpen] = useState(true);
  const [aroundCurrent, setAroundCurrent] = useState(true);
  const [result, setResult] = useState(null); // { key, teams } or { key, error }
  const [loading, setLoading] = useState(false);

  const ownedCount = Object.keys(owned).length;
  const fixed = aroundCurrent ? memberIds : [];
  const full = fixed.length >= TEAM_SIZE;
  const requestKey = JSON.stringify({ owned, gameMode, fixed });
  // Results are only shown for the exact inputs they were asked with
  const current = result?.key === requestKey ? result : null;

  const suggest = async () => {
    setLoading(true);
    const key = requestKey;
    try {
      const body = { owned, gameMode: gameMode || null, mustInclude: fixed, limit: 5 };
      setResult({ key, teams: await getRecommendations(body) });
    } catch (error) {
      setResult({ key, error });
    } finally {
      setLoading(false);
    }
  };

  return (
    <section className="glass-panel mb-8" aria-labelledby="recommendations-title">
      <button type="button" onClick={() => setOpen((v) => !v)} aria-expanded={open}
        className="w-full flex items-center justify-between gap-3 p-4 text-left">
        <span id="recommendations-title" className="flex items-center gap-2 text-lg font-bold text-text-primary">
          <Lightbulb size={20} className="text-accent-gold" /> Suggested teams
        </span>
        <ChevronDown size={20} className={`text-text-muted transition-transform ${open ? "rotate-180" : ""}`} />
      </button>

      {open && (
        <div className="px-4 pb-4 border-t border-border pt-4">
          {ownedCount < MIN_OWNED ? (
            <p className="text-sm text-text-secondary">
              Mark at least {MIN_OWNED} characters in{" "}
              <Link to="/roster" className="text-accent-cyan underline underline-offset-2">My characters</Link>{" "}
              and I'll suggest teams you can actually build.
            </p>
          ) : (
            <>
              <div className="flex flex-wrap items-center gap-3 mb-4">
                <label className="flex items-center gap-2 text-sm">
                  <span className="font-semibold text-text-primary">Game mode</span>
                  <select value={gameMode ?? ""} onChange={(e) => onGameModeChange(e.target.value || null)}
                    className="px-2 py-1.5 rounded-lg border border-border bg-bg-primary text-sm focus:outline-none focus:border-accent-cyan">
                    <option value="">Any</option>
                    {GAME_MODES.map((m) => <option key={m.name} value={m.name}>{m.label}</option>)}
                  </select>
                </label>
                {memberIds.length > 0 && (
                  <label className="flex items-center gap-2 text-sm text-text-secondary cursor-pointer">
                    <input type="checkbox" checked={aroundCurrent} onChange={(e) => setAroundCurrent(e.target.checked)} className="accent-[#1F2A44]" />
                    Build around the {memberIds.length === 1 ? "character" : `${memberIds.length} characters`} already in the team
                  </label>
                )}
                <button type="button" onClick={suggest} disabled={loading || full}
                  className="ml-auto flex items-center gap-2 px-4 py-2 rounded-lg font-semibold bg-text-primary text-bg-card hover:opacity-90 disabled:opacity-40 disabled:cursor-not-allowed">
                  <Wand2 size={16} /> {loading ? "Thinking…" : "Suggest teams"}
                </button>
              </div>

              {full && <p className="text-sm text-text-muted">The team is full. Untick the box to get other ideas.</p>}
              {current?.error && (
                <p className="text-sm text-accent-red" role="alert">
                  {current.error.response?.data?.detail ?? "Couldn't get suggestions. Try again."}
                </p>
              )}
              {current?.teams?.length === 0 && (
                <p className="font-hand text-2xl text-text-secondary">no full team fits these rules with your characters yet</p>
              )}
              {current?.teams?.length > 0 && (
                <ol className="grid gap-4 md:grid-cols-2">
                  {current.teams.map((team, index) => (
                    <SuggestedTeam key={team.members.map((m) => m.id).join("-")} team={team} rank={index + 1} onUse={() => onUse(team)} />
                  ))}
                </ol>
              )}
              <p className="mt-3 text-xs text-text-muted">
                Ranked by synergy from HSRWiki's recommended teams, role balance, the game mode and your Eidolons.
              </p>
            </>
          )}
        </div>
      )}
    </section>
  );
};

const SuggestedTeam = ({ team, rank, onUse }) => (
  <li className="rounded-xl border border-border bg-bg-card p-4 flex flex-col gap-3">
    <div className="flex items-center justify-between gap-2">
      <span className="font-hand text-2xl leading-none text-accent-cyan">option {rank}</span>
      <span className="text-sm font-bold tabular-nums text-text-primary" title={`Synergy ${team.breakdown.synergy} · roles ${team.breakdown.roles} · mode ${team.breakdown.mode} · Eidolons ${team.breakdown.investment}`}>
        score {team.score}
      </span>
    </div>
    <ul className="grid grid-cols-4 gap-2">
      {team.members.map((char, i) => (
        <li key={char.id} className="text-center min-w-0">
          {char.miniIconUrl
            ? <img src={char.miniIconUrl} alt="" loading="lazy" className="w-12 h-12 mx-auto rounded-full bg-bg-elevated object-cover" />
            : <span className="block w-12 h-12 mx-auto rounded-full bg-bg-elevated" />}
          <span className="block mt-1 text-xs font-semibold text-text-primary leading-tight truncate" title={char.name}>{char.name}</span>
          <span className="block text-[11px] text-text-muted truncate">E{team.eidolons[i]} · {roleLabel(char)}</span>
        </li>
      ))}
    </ul>
    <ul className="space-y-1 text-sm text-text-secondary flex-1">
      {team.reasons.map((reason) => (
        <li key={reason} className="flex gap-1.5"><Sparkles size={14} className="mt-0.5 shrink-0 text-accent-gold" /> {reason}</li>
      ))}
    </ul>
    <button type="button" onClick={onUse}
      className="self-start px-3 py-1.5 rounded-lg text-sm font-semibold border border-border bg-bg-secondary text-text-primary hover:bg-bg-elevated">
      Use this team
    </button>
  </li>
);

export default RecommendationsPanel;
