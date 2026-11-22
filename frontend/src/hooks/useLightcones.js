import { getLightcones } from "../services/plannerService";
import { useCachedResource } from "./useCachedResource";

export const useLightcones = () => {
  const { data, loading, error } = useCachedResource("lightcones", getLightcones);
  return { lightcones: data || [], loading, error };
};
