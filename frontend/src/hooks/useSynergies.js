import { useEffect, useState } from "react";
import { getSynergies } from "../services/plannerService";

/**
 * Best partners of one character. The result remembers which character it belongs to,
 * so "loading" is simply "the result is for someone else" and the effect never has to
 * reset state by hand when the detail jumps to another character.
 */
export const useSynergies = (characterId) => {
  const [result, setResult] = useState({ id: null, synergies: [], error: null });

  useEffect(() => {
    let cancelled = false;
    getSynergies(characterId)
      .then((synergies) => { if (!cancelled) setResult({ id: characterId, synergies, error: null }); })
      .catch((error) => { if (!cancelled) setResult({ id: characterId, synergies: [], error }); });
    return () => { cancelled = true; };
  }, [characterId]);

  const current = result.id === characterId;
  return {
    synergies: current ? result.synergies : [],
    loading: !current,
    error: current ? result.error : null,
  };
};
