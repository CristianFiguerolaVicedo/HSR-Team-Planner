import axios from "axios";
import type {
  Character, Element, LightCone, Path, Role, SpdAnalysis, Synergy,
  TeamRequest, TeamResponse, TeamValidation,
} from "./types";

const baseURL =
  import.meta.env.VITE_API_BASE_URL ?? (import.meta.env.DEV ? "http://localhost:8080/api" : undefined);

if (!baseURL) {
  throw new Error("VITE_API_BASE_URL is not set for this build");
}

const http = axios.create({ baseURL, timeout: 30_000 });

export interface CharacterFilters {
  path?: Path;
  element?: Element;
  role?: Role;
  rarity?: number;
}

export const api = {
  characters: (filters: CharacterFilters = {}) =>
    http.get<Character[]>("/characters", { params: filters }).then((r) => r.data),
  character: (id: number) => http.get<Character>(`/characters/${id}`).then((r) => r.data),
  synergies: (id: number, limit = 10) =>
    http.get<Synergy[]>(`/characters/${id}/synergies`, { params: { limit } }).then((r) => r.data),

  lightCones: (filters: { path?: Path; rarity?: number } = {}) =>
    http.get<LightCone[]>("/lightcones", { params: filters }).then((r) => r.data),

  spd: (speed: number, cycles = 3) =>
    http.get<SpdAnalysis>("/spd", { params: { speed, cycles } }).then((r) => r.data),
  teamSpd: (speeds: number[], cycles = 3) =>
    http.post<SpdAnalysis[]>("/spd/team", { speeds, cycles }).then((r) => r.data),

  validateTeam: (team: TeamRequest) =>
    http.post<TeamValidation>("/teams/validate", team).then((r) => r.data),
  createTeam: (team: TeamRequest) => http.post<TeamResponse>("/teams", team).then((r) => r.data),
  team: (shareCode: string) => http.get<TeamResponse>(`/teams/${shareCode}`).then((r) => r.data),
};
