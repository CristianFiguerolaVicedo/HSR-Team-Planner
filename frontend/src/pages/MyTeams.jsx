import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { CalendarDays, LogIn, Pencil, Plus, Trash2 } from "lucide-react";
import { useSession } from "../hooks/useSession";
import { useMyTeams } from "../hooks/useMyTeams";
import { loadDraft } from "../hooks/useTeamDraft";
import { deleteTeam, getTeam } from "../services/plannerService";
import SkeletonLoader from "../components/SkeletonLoader";
import { GAME_MODES, draftFromTeam } from "../util/team";

const MODE_LABEL = Object.fromEntries(GAME_MODES.map((m) => [m.name, m.label]));

/** Teams saved while logged in: open, edit in place (same link) or delete. */
const MyTeams = () => {
  const { status, providers } = useSession();
  const { teams, loaded, error, removeLocally } = useMyTeams(status === "authenticated");
  const [busy, setBusy] = useState(null); // share code being edited or deleted
  const [actionError, setActionError] = useState(null);
  const navigate = useNavigate();

  const edit = async (shareCode) => {
    setBusy(shareCode);
    setActionError(null);
    try {
      // The list only has a summary: load the full team (light cones, SPD...) before editing
      loadDraft(draftFromTeam(await getTeam(shareCode), { inPlace: true }));
      navigate("/builder");
    } catch {
      setActionError("Couldn't open that team. Try again.");
      setBusy(null);
    }
  };

  const remove = async (team) => {
    if (!window.confirm(`Delete "${team.name}"? Its link will stop working for everyone. This can't be undone.`)) return;
    setBusy(team.shareCode);
    setActionError(null);
    try {
      await deleteTeam(team.shareCode);
      removeLocally(team.shareCode);
    } catch (err) {
      setActionError(err.response?.data?.detail ?? "Couldn't delete the team. Try again.");
    } finally {
      setBusy(null);
    }
  };

  const header = (
    <div className="mb-6 flex flex-wrap items-end justify-between gap-4">
      <div>
        <h1 className="text-5xl md:text-6xl font-extrabold tracking-tight text-text-primary">My teams</h1>
        <p className="font-hand text-2xl text-text-secondary mt-1">every ticket you've printed, kept in your account</p>
      </div>
      {status === "authenticated" && (
        <Link to="/builder" className="flex items-center gap-2 px-4 py-2 rounded-lg font-semibold bg-text-primary text-bg-card hover:opacity-90">
          <Plus size={16} /> New team
        </Link>
      )}
    </div>
  );

  if (status === "loading" || (status === "authenticated" && !loaded)) {
    return <>{header}<SkeletonLoader type="card" count={4} /></>;
  }

  if (status === "anonymous") {
    return (
      <div className="text-text-primary">
        {header}
        <div className="glass-panel p-6 max-w-xl space-y-3">
          <p className="text-text-secondary">
            Sign in to keep the teams you save, edit them later without changing their link, and delete them.
            Teams saved without an account still work by their link, but can't be changed.
          </p>
          <div className="flex flex-wrap gap-2">
            {providers.map((p) => (
              <a key={p.id} href={p.loginUrl}
                className="flex items-center gap-2 px-4 py-2 rounded-lg font-semibold bg-text-primary text-bg-card hover:opacity-90">
                <LogIn size={16} /> Continue with {p.name}
              </a>
            ))}
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="text-text-primary">
      {header}
      {error && <p className="mb-4 text-sm text-accent-red" role="alert">Couldn't load your teams. Reload the page to try again.</p>}
      {actionError && <p className="mb-4 text-sm text-accent-red" role="alert">{actionError}</p>}

      {!error && teams.length === 0 ? (
        <div className="py-16 text-center">
          <p className="font-hand text-3xl text-text-secondary">no teams yet</p>
          <Link to="/builder" className="mt-2 inline-block text-sm text-accent-cyan hover:underline">Build your first one</Link>
        </div>
      ) : (
        <ul className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          {teams.map((team) => (
            <li key={team.shareCode} className="glass-panel p-4 flex flex-col gap-3">
              <div className="flex items-start justify-between gap-2">
                <Link to={`/t/${team.shareCode}`} className="min-w-0">
                  <h2 className="text-lg font-bold text-text-primary truncate hover:underline">{team.name}</h2>
                  <p className="flex flex-wrap items-center gap-x-3 text-xs text-text-muted">
                    {team.gameMode && <span className="font-semibold text-text-secondary">{MODE_LABEL[team.gameMode] ?? team.gameMode}</span>}
                    <span className="flex items-center gap-1"><CalendarDays size={12} />{new Date(team.updatedAt ?? team.createdAt).toLocaleDateString()}</span>
                    <span className="tabular-nums">{team.shareCode}</span>
                  </p>
                </Link>
              </div>
              <ul className="flex gap-2" aria-label="Members">
                {team.members.map((char) => (
                  <li key={char.id} title={char.name}>
                    {char.miniIconUrl
                      ? <img src={char.miniIconUrl} alt={char.name} loading="lazy" className="w-11 h-11 rounded-full bg-bg-elevated object-cover" />
                      : <span className="block w-11 h-11 rounded-full bg-bg-elevated" />}
                  </li>
                ))}
              </ul>
              <div className="mt-auto flex gap-2">
                <button type="button" onClick={() => edit(team.shareCode)} disabled={busy === team.shareCode}
                  className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-sm font-semibold border border-border bg-bg-secondary text-text-primary hover:bg-bg-elevated disabled:opacity-40">
                  <Pencil size={14} /> Edit
                </button>
                <button type="button" onClick={() => remove(team)} disabled={busy === team.shareCode}
                  className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-sm text-text-secondary hover:text-accent-red hover:bg-accent-red-muted disabled:opacity-40">
                  <Trash2 size={14} /> Delete
                </button>
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
};

export default MyTeams;
