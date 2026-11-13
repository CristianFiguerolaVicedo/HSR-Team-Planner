import { useEffect, useState } from "react";

/**
 * Calls `fetcher(payload)` when the payload stops changing for `delay` ms.
 * For live checks while the user edits (team validation, SPD): one request per pause, not per keystroke.
 * The result remembers which payload it belongs to, so "loading" is derived instead of set by hand.
 * A null payload means "nothing to ask yet".
 */
export const useDebouncedQuery = (fetcher, payload, delay = 400) => {
  const key = payload == null ? null : JSON.stringify(payload);
  const [result, setResult] = useState({ key: null, data: null, error: null });

  useEffect(() => {
    if (key === null) return undefined;
    let cancelled = false;
    const timer = setTimeout(() => {
      fetcher(JSON.parse(key))
        .then((data) => { if (!cancelled) setResult({ key, data, error: null }); })
        .catch((error) => { if (!cancelled) setResult({ key, data: null, error }); });
    }, delay);
    return () => { cancelled = true; clearTimeout(timer); };
  }, [fetcher, key, delay]);

  if (key === null) return { data: null, error: null, loading: false };
  const fresh = result.key === key;
  // While a new answer is on its way, keep showing the previous one instead of flashing empty
  return { data: result.data, error: fresh ? result.error : null, loading: !fresh };
};
