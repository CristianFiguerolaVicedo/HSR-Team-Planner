import { useCallback, useEffect, useSyncExternalStore } from "react";
import { getAuthProviders, getMe, logout as logoutRequest } from "../services/plannerService";

/*
 * Who is logged in, shared by every component (same store pattern as useRoster / HSRWiki's useCollection).
 *   status: "loading" → asking the server
 *           "anonymous" → no session (the app works as before, with localStorage)
 *           "authenticated" → user = { provider, providerId, name, avatarUrl }
 * The session itself is an HttpOnly cookie the browser sends on its own: JavaScript never sees it,
 * it can only ask the server "who am I?" (GET /api/me).
 */
let state = { status: "loading", user: null, providers: [] };
let started = false;
const listeners = new Set();

const setState = (patch) => {
  state = { ...state, ...patch };
  listeners.forEach((fn) => fn());
};

const subscribe = (fn) => {
  listeners.add(fn);
  return () => listeners.delete(fn);
};

const getSnapshot = () => state;

/** Asks the server who we are. Also makes the server send the XSRF-TOKEN cookie needed for later POSTs. */
export const refreshSession = async () => {
  try {
    const user = await getMe();
    setState({ status: user ? "authenticated" : "anonymous", user });
  } catch {
    setState({ status: "anonymous", user: null });
  }
};

const start = () => {
  if (started) return;
  started = true;
  refreshSession();
  getAuthProviders().then((providers) => setState({ providers })).catch(() => setState({ providers: [] }));
};

export const useSession = () => {
  const session = useSyncExternalStore(subscribe, getSnapshot);
  useEffect(start, []);

  const logout = useCallback(async () => {
    try {
      await logoutRequest();
    } finally {
      // Even if the request failed, stop showing the user as logged in; the next refresh tells the truth
      setState({ status: "anonymous", user: null });
    }
  }, []);

  return { ...session, logout, refresh: refreshSession };
};
