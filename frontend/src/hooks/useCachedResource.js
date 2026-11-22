import { useEffect, useState } from "react";

// Catalog data (characters, light cones) is the same for everyone and only changes once a week.
// So the last copy is kept in the browser: the page shows it instantly, even while the backend
// is still waking up on Render's free plan, and a fresh copy is fetched quietly in the background.
const PREFIX = "hsr-planner-cache-v1:";
const memory = new Map();       // key -> data, shared by every component on the page
const refreshed = new Set();    // keys already refreshed from the server during this visit

const readStored = (key) => {
  try {
    const raw = localStorage.getItem(PREFIX + key);
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
};

const store = (key, data) => {
  try {
    localStorage.setItem(PREFIX + key, JSON.stringify(data));
  } catch {
    // storage full or blocked: the in-memory copy is enough for this visit
  }
};

/**
 * -> { data, loading, error }. `fetcher(quiet)` loads it from the server; quiet = true when a saved
 * copy is already on screen, so the "waking up the server" banner isn't shown for a background refresh.
 */
export const useCachedResource = (key, fetcher) => {
  const [state, setState] = useState(() => {
    const cached = memory.get(key) ?? readStored(key);
    if (cached) memory.set(key, cached);
    return { data: cached, loading: !cached, error: null };
  });

  useEffect(() => {
    if (refreshed.has(key)) {
      if (memory.has(key)) setState({ data: memory.get(key), loading: false, error: null });
      return undefined;
    }
    let cancelled = false;
    const hadCopy = memory.has(key);
    fetcher(hadCopy)
      .then((data) => {
        refreshed.add(key);
        memory.set(key, data);
        store(key, data);
        if (!cancelled) setState({ data, loading: false, error: null });
      })
      .catch((error) => {
        // With a saved copy on screen, a failed refresh isn't worth an error message
        if (!cancelled && !hadCopy) setState({ data: null, loading: false, error });
      });
    return () => { cancelled = true; };
  }, [key, fetcher]);

  return state;
};
