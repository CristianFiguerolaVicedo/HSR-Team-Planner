import { useEffect, useState } from "react";
import { getTeam } from "../services/plannerService";

/** A shared team by its code. notFound is true for a code that doesn't exist (404). */
export const useTeam = (shareCode) => {
  const [result, setResult] = useState({ code: null, team: null, error: null });

  useEffect(() => {
    let cancelled = false;
    getTeam(shareCode)
      .then((team) => { if (!cancelled) setResult({ code: shareCode, team, error: null }); })
      .catch((error) => { if (!cancelled) setResult({ code: shareCode, team: null, error }); });
    return () => { cancelled = true; };
  }, [shareCode]);

  const current = result.code === shareCode;
  return {
    team: current ? result.team : null,
    loading: !current,
    error: current ? result.error : null,
    notFound: current && result.error?.response?.status === 404,
  };
};
