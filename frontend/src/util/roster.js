// Pure helpers behind hooks/useRoster.js, kept apart so node --test can check them without React.

/** Same shape as HSRWiki's collection: { [characterId]: eidolon 0-6 } */
export const EIDOLON_RANGE = [0, 6];

export const clampEidolon = (value) => {
  const [min, max] = EIDOLON_RANGE;
  const n = Math.round(Number(value));
  return Number.isFinite(n) ? Math.min(max, Math.max(min, n)) : min;
};

/** Reads what localStorage holds; anything broken becomes an empty roster instead of a crash. */
export const parseRoster = (raw) => {
  try {
    const parsed = JSON.parse(raw || "null");
    if (!parsed || typeof parsed !== "object" || Array.isArray(parsed)) return {};
    const roster = {};
    for (const [id, eidolon] of Object.entries(parsed)) {
      if (/^\d+$/.test(id)) roster[id] = clampEidolon(eidolon);
    }
    return roster;
  } catch {
    return {};
  }
};

/** changes: { [id]: eidolon | null } where null removes the character. Returns a new object. */
export const applyRosterChanges = (roster, changes) => {
  const next = { ...roster };
  for (const [id, value] of Object.entries(changes)) {
    if (value === null || value === undefined) delete next[String(id)];
    else next[String(id)] = clampEidolon(value);
  }
  return next;
};
