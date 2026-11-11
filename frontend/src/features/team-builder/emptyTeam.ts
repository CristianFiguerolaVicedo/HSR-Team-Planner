import type { TeamRequest } from "../../api/types";

export const TEAM_SIZE = 4;

export function emptyTeam(): TeamRequest {
  return { name: "New team", gameMode: null, notes: null, slots: [] };
}
