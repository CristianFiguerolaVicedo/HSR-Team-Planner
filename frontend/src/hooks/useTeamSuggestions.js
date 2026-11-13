import { useEffect, useState } from "react";
import { getSynergies } from "../services/plannerService";
import { suggestPartners } from "../util/team";

// Synergy lists already downloaded, shared by every builder visit: { [characterId]: Promise<list> }
const cache = new Map();

const synergiesOf = (id) => {
  if (!cache.has(id)) {
    const request = getSynergies(id, 20).catch((err) => {
      cache.delete(id); // let a later visit try again
      throw err;
    });
    cache.set(id, request);
  }
  return cache.get(id);
};

/** Best partners for the current members: [{ id, score }] best first, [] while loading. */
export const useTeamSuggestions = (memberIds) => {
  const key = [...memberIds].sort((a, b) => a - b).join(",");
  const [result, setResult] = useState({ key: "", suggestions: [] });

  useEffect(() => {
    if (!key) return undefined;
    const ids = key.split(",").map(Number);
    let cancelled = false;
    Promise.allSettled(ids.map(synergiesOf)).then((answers) => {
      if (cancelled) return;
      const byMember = {};
      answers.forEach((answer, i) => { byMember[ids[i]] = answer.status === "fulfilled" ? answer.value : []; });
      setResult({ key, suggestions: suggestPartners(ids, byMember) });
    });
    return () => { cancelled = true; };
  }, [key]);

  return key && result.key === key ? result.suggestions : [];
};
