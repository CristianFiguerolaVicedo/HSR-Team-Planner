import { useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { CalendarDays, CopyPlus, Pencil, Swords, Trash2 } from "lucide-react";
import { useTeam } from "../hooks/useTeam";
import { deleteTeam } from "../services/plannerService";
import { loadDraft } from "../hooks/useTeamDraft";
import TeamSlot from "../components/team/TeamSlot";
import ValidationPanel from "../components/team/ValidationPanel";
import { ShareBox } from "../components/team/SaveTeam";
import LoadError from "../components/LoadError";
import SkeletonLoader from "../components/SkeletonLoader";
import { GAME_MODES, TEAM_SIZE, draftFromTeam } from "../util/team";
import { actionsLabel, formatBreakpoint, formatMissing } from "../util/spd";

const MODE_LABEL = Object.fromEntries(GAME_MODES.map((m) => [m.name, m.label]));

/** Read-only view of a team opened from its link: /t/{shareCode}. */
const SharedTeam = () => {
  const { shareCode } = useParams();
  const navigate = useNavigate();
  const { team, loading, error, notFound } = useTeam(shareCode);
  const [deleting, setDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState(null);

  if (loading) return <SkeletonLoader type="card" count={4} />;
  if (notFound) {
    return (
      <div className="glass-panel p-8 max-w-xl text-text-primary">
        <p className="font-hand text-3xl text-accent-red mb-2">no ticket with that code</p>
        <p className="text-text-secondary mb-4">The link may be mistyped. Codes have 8 letters and numbers, like <code>aZ3kP9qX</code>.</p>
        <Link to="/builder" className="inline-flex items-center gap-2 px-4 py-2 rounded-lg font-semibold bg-text-primary text-bg-card">
          <Swords size={16} /> Build a team
        </Link>
      </div>
    );
  }
  if (error) return <LoadError error={error} what="this team" />;

  const slotsByPosition = Object.fromEntries(team.slots.map((s) => [s.position, s]));
  const openInBuilder = (inPlace) => {
    loadDraft(draftFromTeam(team, { inPlace }));
    navigate("/builder");
  };

  const remove = async () => {
    if (!window.confirm(`Delete "${team.name}"? Its link will stop working for everyone. This can't be undone.`)) return;
    setDeleting(true);
    setDeleteError(null);
    try {
      await deleteTeam(team.shareCode);
      navigate("/my-teams");
    } catch (err) {
      setDeleteError(err.response?.data?.detail ?? "Couldn't delete the team. Try again.");
      setDeleting(false);
    }
  };

  return (
    <div className="text-text-primary">
      <div className="mb-6 flex flex-wrap items-end justify-between gap-4">
        <div>
          <p className="font-hand text-2xl text-accent-cyan">ticket {team.shareCode}</p>
          <h1 className="text-4xl md:text-5xl font-extrabold tracking-tight">{team.name}</h1>
          <p className="mt-2 flex flex-wrap items-center gap-x-4 gap-y-1 text-sm text-text-secondary">
            {team.gameMode && <span className="px-2 py-0.5 rounded-md bg-[#FFE68F] border border-[#E3C552] text-text-primary font-semibold">{MODE_LABEL[team.gameMode] ?? team.gameMode}</span>}
            <span className="flex items-center gap-1"><CalendarDays size={14} /> {new Date(team.createdAt).toLocaleDateString()}</span>
            {team.updatedAt && <span>updated {new Date(team.updatedAt).toLocaleDateString()}</span>}
            {team.editable && <span className="font-semibold text-accent-cyan">your team</span>}
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          {team.editable && (
            <>
              <button type="button" onClick={() => openInBuilder(true)}
                className="flex items-center gap-2 px-4 py-2 rounded-lg font-semibold bg-text-primary text-bg-card hover:opacity-90">
                <Pencil size={16} /> Edit
              </button>
              <button type="button" onClick={remove} disabled={deleting}
                className="flex items-center gap-2 px-3 py-2 rounded-lg text-sm text-text-secondary hover:text-accent-red hover:bg-accent-red-muted disabled:opacity-40">
                <Trash2 size={16} /> {deleting ? "Deleting…" : "Delete"}
              </button>
            </>
          )}
          <button type="button" onClick={() => openInBuilder(false)}
            className={team.editable
              ? "flex items-center gap-2 px-3 py-2 rounded-lg text-sm border border-border bg-bg-card text-text-primary hover:bg-bg-elevated"
              : "flex items-center gap-2 px-4 py-2 rounded-lg font-semibold bg-text-primary text-bg-card hover:opacity-90"}>
            {team.editable ? <CopyPlus size={16} /> : <Pencil size={16} />} Edit a copy
          </button>
        </div>
      </div>

      {deleteError && <p className="mb-4 text-sm text-accent-red" role="alert">{deleteError}</p>}

      {team.notes && (
        <p className="glass-panel p-4 mb-6 whitespace-pre-line text-text-primary">{team.notes}</p>
      )}

      <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-x-5 gap-y-8 pt-3 mb-8">
        {Array.from({ length: TEAM_SIZE }, (_, position) => {
          const slot = slotsByPosition[position];
          return (
            <TeamSlot key={position} position={position} readOnly
              slot={slot} char={slot?.character} lightcone={slot?.lightCone} spdAnalysis={slot?.spdAnalysis}
              issues={team.validation.issues.filter((i) => i.position === position)} />
          );
        })}
      </div>

      <div className="grid gap-6 lg:grid-cols-[1fr_380px] items-start">
        <div className="space-y-6">
          <SpdTable slots={team.slots} />
          <ValidationPanel validation={team.validation} />
        </div>
        <ShareBox code={team.shareCode} />
      </div>
    </div>
  );
};

/** Turn order of the first cycles for every slot with a SPD. */
const SpdTable = ({ slots }) => {
  const rows = slots.filter((s) => s.spdAnalysis).sort((a, b) => b.spd - a.spd);
  if (rows.length === 0) return null;
  const cycles = rows[0].spdAnalysis.cycles.map((c) => c.cycle);

  return (
    <section className="glass-panel p-5 overflow-x-auto" aria-labelledby="spd-title">
      <h2 id="spd-title" className="text-lg font-bold mb-3">Actions per cycle</h2>
      <table className="w-full text-sm">
        <thead>
          <tr className="text-left text-text-muted">
            <th className="py-1 pr-3 font-semibold">Character</th>
            <th className="py-1 pr-3 font-semibold">SPD</th>
            {cycles.map((c) => <th key={c} className="py-1 pr-3 font-semibold">Cycle {c}</th>)}
            <th className="py-1 font-semibold">Next in cycle 0</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((slot) => {
            const zero = slot.spdAnalysis.cycles[0];
            return (
              <tr key={slot.position} className="border-t border-border">
                <td className="py-1.5 pr-3 font-semibold">{slot.character.name}</td>
                <td className="py-1.5 pr-3 tabular-nums">{slot.spd}</td>
                {slot.spdAnalysis.cycles.map((c) => <td key={c.cycle} className="py-1.5 pr-3">{actionsLabel(c.actions)}</td>)}
                <td className="py-1.5 tabular-nums text-text-secondary">
                  {formatBreakpoint(zero.nextBreakpoint)} <span className="text-accent-cyan">({formatMissing(zero.spdMissing)})</span>
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </section>
  );
};

export default SharedTeam;
