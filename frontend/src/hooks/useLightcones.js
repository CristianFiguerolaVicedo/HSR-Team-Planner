import { useState, useEffect } from "react";
import { getLightcones } from "../services/plannerService";

let cachedLightcones = null;

export const useLightcones = () => {
  const [state, setState] = useState(() => ({
    lightcones: cachedLightcones || [],
    loading: !cachedLightcones,
    error: null,
  }));

  useEffect(() => {
    if (cachedLightcones) return;

    let cancelled = false;

    const load = async () => {
      try {
        const data = await getLightcones();
        if (!cancelled) {
          cachedLightcones = data;
          setState({ lightcones: data, loading: false, error: null });
        }
      } catch (err) {
        if (!cancelled) {
          setState({ lightcones: [], loading: false, error: err });
        }
      }
    };

    load();
    return () => { cancelled = true; };
  }, []);

  return state;
};
