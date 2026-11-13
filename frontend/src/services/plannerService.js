import { fetchJson, postJson } from "./apiClient";

// One function per backend endpoint (see README → API)

export const getCharacters = () => fetchJson("/characters");

export const getSynergies = (characterId, limit = 8) =>
  fetchJson(`/characters/${characterId}/synergies?limit=${limit}`);

export const getLightcones = () => fetchJson("/lightcones");

export const getSpd = (speed, cycles = 3) => fetchJson(`/spd?speed=${speed}&cycles=${cycles}`);

export const getTeamSpd = (speeds, cycles = 3) => postJson("/spd/team", { speeds, cycles });

export const validateTeam = (team) => postJson("/teams/validate", team);

export const createTeam = (team) => postJson("/teams", team);

export const getTeam = (shareCode) => fetchJson(`/teams/${encodeURIComponent(shareCode)}`);

/** body: { owned: { [id]: eidolon }, gameMode, mustInclude: [ids], limit } */
export const getRecommendations = (body) => postJson("/recommendations", body);
