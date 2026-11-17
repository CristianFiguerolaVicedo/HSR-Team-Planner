import { useEffect, useState } from "react";
import { AlertTriangle } from "lucide-react";
import Modal from "./Modal";
import { useSession } from "../hooks/useSession";
import { useRoster } from "../hooks/useRoster";
import { getMyTeams } from "../services/plannerService";

const CONFIRM_WORD = "DELETE";

/**
 * "Delete my account": says exactly what will be lost (characters, teams and their links) and asks
 * the user to type DELETE, so it can't happen with one stray click.
 */
const DeleteAccountDialog = ({ onClose }) => {
  const { user, deleteAccount } = useSession();
  const { owned, source } = useRoster();
  const [teamCount, setTeamCount] = useState(null);
  const [typed, setTyped] = useState("");
  const [deleting, setDeleting] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    let cancelled = false;
    getMyTeams()
      .then((teams) => { if (!cancelled) setTeamCount(teams.length); })
      .catch(() => { if (!cancelled) setTeamCount(-1); });
    return () => { cancelled = true; };
  }, []);

  const characterCount = source === "account" ? Object.keys(owned).length : null;

  const confirm = async () => {
    setDeleting(true);
    setError(null);
    try {
      await deleteAccount();
      onClose();
    } catch {
      setError("Couldn't delete the account. Nothing was deleted; try again.");
      setDeleting(false);
    }
  };

  return (
    <Modal title="Delete my account" onClose={onClose}
      footer={
        <div className="flex flex-wrap justify-end gap-2">
          <button type="button" onClick={onClose}
            className="px-4 py-2 rounded-lg text-sm text-text-secondary hover:text-text-primary hover:bg-bg-elevated">
            Cancel
          </button>
          <button type="button" onClick={confirm} disabled={typed !== CONFIRM_WORD || deleting}
            className="px-4 py-2 rounded-lg text-sm font-semibold bg-accent-red text-white hover:opacity-90 disabled:opacity-40 disabled:cursor-not-allowed">
            {deleting ? "Deleting…" : "Delete everything"}
          </button>
        </div>
      }>
      <div className="space-y-4 text-sm text-text-primary">
        <p className="flex gap-2">
          <AlertTriangle size={18} className="shrink-0 text-accent-red" aria-hidden="true" />
          <span>
            This deletes the account <span className="font-semibold">{user?.name}</span> and everything saved in it.
            It can't be undone.
          </span>
        </p>
        <ul className="list-disc pl-6 space-y-1 text-text-secondary">
          <li>
            {characterCount === null ? "Your saved characters" : `${characterCount} saved character${characterCount === 1 ? "" : "s"}`}
          </li>
          <li>
            {teamCount === null && "Your teams (counting…)"}
            {teamCount === -1 && "All your teams"}
            {teamCount >= 0 && `${teamCount} team${teamCount === 1 ? "" : "s"}`}
            <span className="font-semibold text-text-primary"> — their share links will stop working for everyone</span>
          </li>
        </ul>
        <p className="text-text-secondary">
          Characters kept in this browser (from before you signed in) stay here; you can clear them on the privacy page.
        </p>
        <label className="block">
          Type <span className="font-mono font-semibold">{CONFIRM_WORD}</span> to confirm
          <input type="text" value={typed} onChange={(e) => setTyped(e.target.value)} autoComplete="off" autoFocus
            className="mt-1 w-full px-3 py-2 rounded-lg border border-border bg-bg-primary font-mono focus:outline-none focus:border-accent-red" />
        </label>
        {error && <p className="text-accent-red" role="alert">{error}</p>}
      </div>
    </Modal>
  );
};

export default DeleteAccountDialog;
