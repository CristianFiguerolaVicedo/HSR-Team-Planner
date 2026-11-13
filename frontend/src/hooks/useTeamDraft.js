import { useCallback, useEffect, useState } from "react";
import { addCharacterToDraft, emptyDraft, newSlot } from "../util/team";

/*
 * Team being built, saved in this browser so a reload or a visit to another page doesn't lose it.
 * Saved teams are immutable on the server: editing a shared team loads a copy here (loadDraft).
 */
const KEY = "hsr-planner-draft";

const readDraft = () => {
  try {
    const parsed = JSON.parse(localStorage.getItem(KEY) || "null");
    if (parsed && Array.isArray(parsed.slots) && parsed.slots.length === 4) return { ...emptyDraft(), ...parsed };
  } catch { /* broken or blocked storage: start fresh */ }
  return emptyDraft();
};

/** Replaces the saved draft, e.g. "Edit a copy" on a shared team, before navigating to the builder. */
export const loadDraft = (draft) => {
  try { localStorage.setItem(KEY, JSON.stringify(draft)); } catch { /* nothing to do */ }
};

/**
 * @param incoming optional { characterId, eidolon } to drop into the first free slot when the page opens
 *                 (HSRWiki's "Plan a team with X" opens /builder?with=1102). Only read on the first render.
 */
export const useTeamDraft = (incoming) => {
  const [draft, setDraft] = useState(() =>
    incoming?.characterId ? addCharacterToDraft(readDraft(), incoming.characterId, incoming.eidolon ?? 0) : readDraft());

  useEffect(() => {
    try { localStorage.setItem(KEY, JSON.stringify(draft)); } catch { /* keep it for this visit */ }
  }, [draft]);

  const setField = useCallback((field, value) => setDraft((d) => ({ ...d, [field]: value })), []);

  const updateSlot = useCallback((position, patch) => setDraft((d) => {
    const slots = [...d.slots];
    slots[position] = slots[position] && { ...slots[position], ...patch };
    return { ...d, slots };
  }), []);

  const placeCharacter = useCallback((position, characterId, eidolon) => setDraft((d) => {
    const slots = [...d.slots];
    const previous = slots[position];
    // Swapping who sits in a slot keeps nothing of the old build
    slots[position] = previous?.characterId === characterId ? previous : newSlot(characterId, eidolon ?? 0);
    return { ...d, slots };
  }), []);

  const clearSlot = useCallback((position) => setDraft((d) => {
    const slots = [...d.slots];
    slots[position] = null;
    return { ...d, slots };
  }), []);

  /** Replaces the 4 slots at once (a recommended team), keeping name, mode and notes. */
  const setSlots = useCallback((slots) => setDraft((d) => ({ ...d, slots })), []);

  const reset = useCallback(() => setDraft(emptyDraft()), []);

  return { draft, setField, updateSlot, placeCharacter, clearSlot, setSlots, reset };
};
