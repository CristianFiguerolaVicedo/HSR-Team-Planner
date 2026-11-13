import { useCallback, useSyncExternalStore } from "react";
import { applyRosterChanges, parseRoster } from "../util/roster";

/*
 * "My characters", kept in this browser only (no accounts in the MVP).
 * Same store pattern as HSRWiki's useCollection: one cached object shared by every component,
 * kept in step between tabs through the "storage" event.
 *   roster: { [characterId]: eidolon 0-6 }
 * Phase 2: filled from the player's UID instead of by hand.
 */
const KEY = "hsr-planner-roster";
const EMPTY = {};
const listeners = new Set();
let cache = null;

const read = () => {
  if (cache) return cache;
  try {
    cache = parseRoster(localStorage.getItem(KEY));
  } catch {
    cache = {}; // storage blocked
  }
  return cache;
};

const write = (data) => {
  cache = data;
  try { localStorage.setItem(KEY, JSON.stringify(data)); } catch { /* private mode: keep it for this visit */ }
  listeners.forEach((fn) => fn());
};

const subscribe = (fn) => {
  listeners.add(fn);
  const onStorage = (e) => { if (e.key === KEY) { cache = null; fn(); } };
  window.addEventListener("storage", onStorage);
  return () => { listeners.delete(fn); window.removeEventListener("storage", onStorage); };
};

const update = (changes) => write(applyRosterChanges(read(), changes));

/** useRoster() -> { owned, has(id), eidolon(id), set(id, e), remove(id), toggle(id), setMany(ids, bool), merge(changes), clear() } */
export const useRoster = () => {
  const owned = useSyncExternalStore(subscribe, read, () => EMPTY);
  const has = useCallback((id) => String(id) in owned, [owned]);
  const eidolon = useCallback((id) => owned[String(id)], [owned]);
  const set = useCallback((id, value) => update({ [id]: value }), []);
  const remove = useCallback((id) => update({ [id]: null }), []);
  const toggle = useCallback((id) => update({ [id]: String(id) in read() ? null : 0 }), []);
  const setMany = useCallback((ids, value) => {
    const current = read();
    // Adding keeps the eidolon of characters already on the list
    update(Object.fromEntries(ids.map((id) => [id, value ? (current[String(id)] ?? 0) : null])));
  }, []);
  /** Writes several Eidolons at once ({ [id]: eidolon }), e.g. an import from UID. */
  const merge = useCallback((changes) => update(changes), []);
  const clear = useCallback(() => write({}), []);
  return { owned, has, eidolon, set, remove, toggle, setMany, merge, clear };
};
