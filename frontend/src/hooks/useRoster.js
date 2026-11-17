import { useCallback, useSyncExternalStore } from "react";
import { API_BASE_URL } from "../services/apiClient";
import { putMyRoster } from "../services/plannerService";
import { applyRosterChanges, parseRoster } from "../util/roster";

/*
 * "My characters": { [characterId]: eidolon 0-6 }, shared by every component
 * (same store pattern as HSRWiki's useCollection).
 *
 * Two places it can live:
 *   - "local":   this browser's localStorage. Used while nobody is logged in.
 *   - "account": the logged-in user's roster on the server. components/RosterSync.jsx switches to it
 *                after loading GET /api/me/roster, and back to "local" on logout.
 * Components don't care: they read `owned` and call set/remove/toggle as before.
 *
 * In "account" mode every change is shown at once and saved a moment later (PUT of the whole roster).
 * Only one PUT is in flight at a time; changes made meanwhile are sent right after, so the server
 * always ends with the last state. The localStorage roster is never touched while logged in: it's
 * still there after logging out.
 */
const KEY = "hsr-planner-roster";
const SAVE_DELAY_MS = 600;
const RETRY_DELAY_MS = 10_000;
const EMPTY = {};

const listeners = new Set();
const notify = () => listeners.forEach((fn) => fn());

let localCache = null;
let account = null; // { userKey, roster } while logged in
let saveTimer = null;
let inFlight = null; // promise of the PUT being sent
let dirty = false; // changed since the last PUT started
let status = { source: "local", saving: "idle" }; // saving: "idle" | "saving" | "error"

const setStatus = (patch) => {
  status = { ...status, ...patch };
  notify();
};

// --- local storage ------------------------------------------------------------------------------

export const readLocalRoster = () => {
  if (localCache) return localCache;
  try {
    localCache = parseRoster(localStorage.getItem(KEY));
  } catch {
    localCache = {}; // storage blocked
  }
  return localCache;
};

const writeLocal = (data) => {
  localCache = data;
  try { localStorage.setItem(KEY, JSON.stringify(data)); } catch { /* private mode: keep it for this visit */ }
};

// --- account ------------------------------------------------------------------------------------

const flush = () => {
  clearTimeout(saveTimer);
  saveTimer = null;
  if (!account || inFlight) return inFlight ?? Promise.resolve();
  const { userKey, roster } = account;
  dirty = false;
  setStatus({ saving: "saving" });
  inFlight = putMyRoster(roster)
    .then((saved) => {
      inFlight = null;
      if (account?.userKey !== userKey) return;
      if (dirty) return flush(); // changed while saving: send the newer state
      // The server drops ids the catalog doesn't know; take its answer as the truth
      account = { userKey, roster: parseRoster(JSON.stringify(saved)) };
      setStatus({ saving: "idle" });
    })
    .catch(() => {
      inFlight = null;
      if (account?.userKey !== userKey) return;
      dirty = true;
      setStatus({ saving: "error" });
      saveTimer = setTimeout(flush, RETRY_DELAY_MS);
    });
  return inFlight;
};

const scheduleSave = () => {
  dirty = true;
  clearTimeout(saveTimer);
  saveTimer = setTimeout(flush, SAVE_DELAY_MS);
};

/** Switches to the account roster (called by RosterSync once GET /api/me/roster answered). */
export const startAccountRoster = (userKey, roster) => {
  clearTimeout(saveTimer);
  saveTimer = null;
  dirty = false;
  account = { userKey, roster };
  setStatus({ source: "account", saving: "idle" });
};

/** Back to this browser's roster (logout). */
export const stopAccountRoster = () => {
  if (!account) return;
  clearTimeout(saveTimer);
  saveTimer = null;
  dirty = false;
  account = null;
  setStatus({ source: "local", saving: "idle" });
};

/** Sends any pending change now, e.g. right before logging out. */
export const flushAccountRoster = () => (account && (dirty || inFlight) ? flush() : Promise.resolve());

// Closing the tab right after a change: send it anyway. fetch with keepalive survives the page
// unloading (axios can't), so the CSRF header is copied from its cookie by hand.
if (typeof window !== "undefined") {
  window.addEventListener("pagehide", () => {
    if (!account || !dirty) return;
    const xsrf = document.cookie.match(/(?:^|; )XSRF-TOKEN=([^;]*)/)?.[1];
    fetch(`${API_BASE_URL}/me/roster`, {
      method: "PUT",
      keepalive: true,
      credentials: "same-origin",
      headers: { "Content-Type": "application/json", ...(xsrf ? { "X-XSRF-TOKEN": decodeURIComponent(xsrf) } : {}) },
      body: JSON.stringify(account.roster),
    }).catch(() => {});
  });
}

// --- store --------------------------------------------------------------------------------------

const read = () => (account ? account.roster : readLocalRoster());

const write = (data) => {
  if (account) {
    account = { ...account, roster: data };
    scheduleSave();
  } else {
    writeLocal(data);
  }
  notify();
};

const subscribe = (fn) => {
  listeners.add(fn);
  // Other tabs changing the local roster (only matters while logged out)
  const onStorage = (e) => {
    if (e.key === KEY) {
      localCache = null;
      if (!account) fn();
    }
  };
  window.addEventListener("storage", onStorage);
  return () => { listeners.delete(fn); window.removeEventListener("storage", onStorage); };
};

const update = (changes) => write(applyRosterChanges(read(), changes));
const getStatus = () => status;

/**
 * useRoster() -> { owned, has(id), eidolon(id), set(id, e), remove(id), toggle(id), setMany(ids, bool),
 *                  merge(changes), clear(), source: "local" | "account", saving: "idle" | "saving" | "error" }
 */
export const useRoster = () => {
  const owned = useSyncExternalStore(subscribe, read, () => EMPTY);
  const { source, saving } = useSyncExternalStore(subscribe, getStatus);
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
  return { owned, has, eidolon, set, remove, toggle, setMany, merge, clear, source, saving };
};
