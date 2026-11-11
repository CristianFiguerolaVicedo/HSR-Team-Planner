// Mirrors the backend DTOs (backend/src/main/java/com/crifigvic/hsrplanner/**/dto).

export type Element = "PHYSICAL" | "FIRE" | "ICE" | "LIGHTNING" | "WIND" | "QUANTUM" | "IMAGINARY";
export type Path =
  | "DESTRUCTION" | "HUNT" | "ERUDITION" | "HARMONY" | "NIHILITY"
  | "PRESERVATION" | "ABUNDANCE" | "REMEMBRANCE" | "ELATION";
export type Role = "MAIN_DPS" | "SUB_DPS" | "AMPLIFIER" | "SUSTAIN";
export type GameMode =
  | "MEMORY_OF_CHAOS" | "PURE_FICTION" | "APOCALYPTIC_SHADOW" | "ANOMALY_ARBITRATION" | "OVERWORLD";

export interface Character {
  id: number;
  name: string;
  rarity: number;
  element: Element;
  path: Path;
  role: Role;
  baseSpd: number;
  variantGroup: string | null;
  iconUrl: string | null;
  miniIconUrl: string | null;
}

export interface LightCone {
  id: number;
  name: string;
  rarity: number;
  path: Path;
  iconUrl: string | null;
}

export interface Synergy {
  partner: Character;
  score: number;
  source: "BUILD_GUIDE" | "CURATED";
  reason: string | null;
}

export interface CycleBreakdown {
  cycle: number;
  actions: number;
  totalActions: number;
  nextBreakpoint: number;
  spdMissing: number;
}

export interface SpdAnalysis {
  spd: number;
  actionValue: number;
  cycles: CycleBreakdown[];
}

export interface TeamSlotRequest {
  position: number;
  characterId: number;
  lightConeId?: number | null;
  eidolon: number;
  superimposition?: number | null;
  spd?: number | null;
}

export interface TeamRequest {
  name: string;
  gameMode?: GameMode | null;
  notes?: string | null;
  slots: TeamSlotRequest[];
}

export type Severity = "ERROR" | "WARNING" | "INFO";

export interface ValidationIssue {
  code: string;
  severity: Severity;
  position: number | null;
  message: string;
}

export interface SynergyPair {
  characterId: number;
  characterName: string;
  otherCharacterId: number;
  otherCharacterName: string;
  score: number;
}

export interface TeamValidation {
  valid: boolean;
  issues: ValidationIssue[];
  roles: Record<Role, number>;
  synergyPairs: SynergyPair[];
  synergyScore: number;
}

export interface TeamSlotResponse {
  position: number;
  character: Character;
  lightCone: LightCone | null;
  eidolon: number;
  superimposition: number | null;
  spd: number | null;
  spdAnalysis: SpdAnalysis | null;
}

export interface TeamResponse {
  shareCode: string;
  name: string;
  gameMode: GameMode | null;
  notes: string | null;
  createdAt: string;
  slots: TeamSlotResponse[];
  validation: TeamValidation;
}
