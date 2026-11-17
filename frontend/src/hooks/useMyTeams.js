import { useCallback, useEffect, useState } from "react";
import { getMyTeams } from "../services/plannerService";

/** Teams saved while logged in. Only asks the server when `enabled` (there is a session). */
export const useMyTeams = (enabled) => {
  const [result, setResult] = useState({ teams: [], loaded: false, error: null });

  useEffect(() => {
    if (!enabled) return undefined;
    let cancelled = false;
    getMyTeams()
      .then((teams) => { if (!cancelled) setResult({ teams, loaded: true, error: null }); })
      .catch((error) => { if (!cancelled) setResult({ teams: [], loaded: true, error }); });
    return () => { cancelled = true; };
  }, [enabled]);

  /** Drops one team from the list without asking the server again (after deleting it). */
  const removeLocally = useCallback((shareCode) =>
    setResult((r) => ({ ...r, teams: r.teams.filter((t) => t.shareCode !== shareCode) })), []);

  return { ...result, removeLocally };
};
