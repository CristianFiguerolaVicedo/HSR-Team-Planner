// Pure logic of "Import from UID", kept apart so node --test can check it without React.

/** Same rule as the backend: 9 or 10 digits, not starting with 0. */
export const isValidUid = (uid) => /^[1-9]\d{8,9}$/.test(uid.trim());

/**
 * Compares an import with the roster ({ [id]: eidolon }).
 * status: "new" (not in the list), "changed" (different Eidolon) or "same".
 * changes: what to write to the roster to match the game, { [id]: eidolon }.
 */
export const compareImport = (imported, owned) => {
  const rows = imported.map(({ character, eidolon, level }) => {
    const current = owned[String(character.id)];
    const status = current === undefined ? "new" : current !== eidolon ? "changed" : "same";
    return { character, eidolon, level, current, status };
  });
  const changes = Object.fromEntries(rows.filter((r) => r.status !== "same").map((r) => [r.character.id, r.eidolon]));
  return { rows, changes, newCount: rows.filter((r) => r.status === "new").length, changedCount: rows.filter((r) => r.status === "changed").length };
};
