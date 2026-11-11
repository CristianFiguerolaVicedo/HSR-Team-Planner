import { useCallback, useEffect, useState } from "react";

/**
 * "My characters": ids of the characters the user owns, kept in localStorage.
 * Versioned key so the format can change later (e.g. to store eidolons) without breaking old data.
 * Phase 2: filled from the player's UID instead of by hand.
 */
const STORAGE_KEY = "hsr-planner:roster:v1";

function readRoster(): Set<number> {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    const ids: unknown = raw ? JSON.parse(raw) : [];
    return new Set(Array.isArray(ids) ? ids.filter((id): id is number => Number.isInteger(id)) : []);
  } catch {
    // Private mode, quota or corrupt data: start empty instead of crashing
    return new Set();
  }
}

export function useRoster() {
  const [owned, setOwned] = useState<Set<number>>(readRoster);

  useEffect(() => {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify([...owned]));
    } catch {
      // Nothing to do: the roster still works for this session
    }
  }, [owned]);

  const toggle = useCallback((id: number) => {
    setOwned((current) => {
      const next = new Set(current);
      if (next.has(id)) {
        next.delete(id);
      } else {
        next.add(id);
      }
      return next;
    });
  }, []);

  const setAll = useCallback((ids: number[]) => setOwned(new Set(ids)), []);

  return { owned, has: (id: number) => owned.has(id), toggle, setAll };
}
