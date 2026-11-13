import { fetchJson, getOnce, postJson } from "./apiClient";

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

/** Characters in a player's in-game showcase (Enka.Network, Mihomo as fallback). Not retried. */
export const importFromUid = (uid) => getOnce(`/import/${encodeURIComponent(uid)}`);

// --- account (login with Discord / Google) ---------------------------------------------------

/** Login buttons to show: [{ id, name, loginUrl }]. Empty when no provider is configured. */
export const getAuthProviders = () => fetchJson("/auth/providers");

/** The logged-in user ({ provider, providerId, name, avatarUrl }), or null without a session. */
export const getMe = async () => {
  try {
    return await getOnce("/me");
  } catch (err) {
    if (err.response?.status === 401) return null;
    throw err;
  }
};

/** Ends the session on the server (POST, so it carries the CSRF token). */
export const logout = () => postJson("/logout");
