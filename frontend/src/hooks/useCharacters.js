import { getCharacters } from "../services/plannerService";
import { useCachedResource } from "./useCachedResource";

export const useCharacters = () => {
  const { data, loading, error } = useCachedResource("characters", getCharacters);
  return { characters: data || [], loading, error };
};
