import { useEffect, useState } from "react";
import { CloudOff, UploadCloud } from "lucide-react";
import { useSession } from "../hooks/useSession";
import { readLocalRoster, startAccountRoster, stopAccountRoster, useRoster } from "../hooks/useRoster";
import { getMyRoster } from "../services/plannerService";
import { localExtras, parseRoster } from "../util/roster";

// Per account, so each user is asked once whether to add this browser's characters
const askedKey = (userKey) => `hsr-planner-roster-merge-asked:${userKey}`;
const wasAsked = (userKey) => { try { return localStorage.getItem(askedKey(userKey)) === "1"; } catch { return true; } };
const markAsked = (userKey) => { try { localStorage.setItem(askedKey(userKey), "1"); } catch { /* ask again next time */ } };

/**
 * Connects "My characters" to the account. Renders nothing most of the time:
 * - logged in: loads GET /api/me/roster and switches useRoster to it;
 * - first login with characters only in this browser: offers to add them to the account;
 * - logged out: back to the browser's roster;
 * - shows a notice when the account can't be loaded or saved.
 */
const RosterSync = () => {
  const { status, user } = useSession();
  const { merge, saving } = useRoster();
  const userKey = user ? `${user.provider}:${user.providerId}` : null;
  const [offer, setOffer] = useState(null); // { userKey, changes }
  const [loadFailed, setLoadFailed] = useState(null); // userKey whose roster couldn't load

  useEffect(() => {
    if (status === "anonymous") {
      stopAccountRoster();
      return undefined;
    }
    if (status !== "authenticated" || !userKey) return undefined;

    let cancelled = false;
    getMyRoster()
      .then((server) => {
        if (cancelled) return;
        const accountRoster = parseRoster(JSON.stringify(server));
        startAccountRoster(userKey, accountRoster);
        setLoadFailed(null);
        const changes = localExtras(readLocalRoster(), accountRoster);
        if (Object.keys(changes).length > 0 && !wasAsked(userKey)) setOffer({ userKey, changes });
      })
      .catch(() => { if (!cancelled) setLoadFailed(userKey); });
    return () => { cancelled = true; };
  }, [status, userKey]);

  const answer = (accept) => {
    if (accept) merge(offer.changes);
    markAsked(offer.userKey);
    setOffer(null);
  };

  const showOffer = offer && offer.userKey === userKey;
  const count = showOffer ? Object.keys(offer.changes).length : 0;

  return (
    <>
      {showOffer && (
        <div className="max-w-7xl mx-auto mt-4 px-4" role="dialog" aria-labelledby="merge-title">
          <div className="glass-panel p-4 flex flex-wrap items-center gap-3">
            <UploadCloud size={22} className="text-accent-cyan shrink-0" aria-hidden="true" />
            <p id="merge-title" className="flex-1 min-w-60 text-sm text-text-primary">
              This browser has <span className="font-semibold">{count} character{count === 1 ? "" : "s"}</span> (or
              higher Eidolons) that your account doesn't. Add them to your account?
            </p>
            <button type="button" onClick={() => answer(true)}
              className="px-4 py-2 rounded-lg text-sm font-semibold bg-text-primary text-bg-card hover:opacity-90">
              Add to my account
            </button>
            <button type="button" onClick={() => answer(false)}
              className="px-3 py-2 rounded-lg text-sm text-text-secondary hover:text-text-primary hover:bg-bg-elevated">
              Keep my account as it is
            </button>
          </div>
        </div>
      )}

      {(saving === "error" || (loadFailed && loadFailed === userKey)) && (
        <div role="status" className="fixed bottom-4 right-4 z-50 max-w-sm glass-panel px-4 py-3 flex items-start gap-3 shadow-lg">
          <CloudOff size={18} className="mt-0.5 shrink-0 text-accent-red" aria-hidden="true" />
          <p className="text-sm text-text-primary">
            {saving === "error"
              ? "Couldn't save your characters to your account. Retrying in a few seconds…"
              : "Couldn't load your characters from your account. Changes are kept in this browser for now."}
          </p>
        </div>
      )}
    </>
  );
};

export default RosterSync;
