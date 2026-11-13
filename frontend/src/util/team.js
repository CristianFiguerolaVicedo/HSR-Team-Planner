// Pure logic of the team builder, kept apart so node --test can check it without React.

export const TEAM_SIZE = 4;

export const GAME_MODES = [
  { name: "MEMORY_OF_CHAOS", label: "Memory of Chaos" },
  { name: "PURE_FICTION", label: "Pure Fiction" },
  { name: "APOCALYPTIC_SHADOW", label: "Apocalyptic Shadow" },
  { name: "ANOMALY_ARBITRATION", label: "Anomaly Arbitration" },
  { name: "OVERWORLD", label: "Overworld" },
];

/**
 * Draft being edited. slots always has 4 entries; an empty one is null.
 * slot: { characterId, eidolon, lightConeId, superimposition, spd }
 */
export const emptyDraft = () => ({
  name: "",
  gameMode: null,
  notes: "",
  slots: Array(TEAM_SIZE).fill(null),
});

export const newSlot = (characterId, eidolon = 0) => ({
  characterId,
  eidolon,
  lightConeId: null,
  superimposition: 1,
  spd: null,
});

export const filledSlots = (draft) => draft.slots.filter(Boolean);

/** Body for POST /teams and /teams/validate. Null when there's nothing to send yet. */
export const toTeamRequest = (draft) => {
  const slots = draft.slots
    .map((slot, position) => slot && {
      position,
      characterId: slot.characterId,
      lightConeId: slot.lightConeId ?? null,
      eidolon: slot.eidolon ?? 0,
      superimposition: slot.lightConeId ? (slot.superimposition ?? 1) : null,
      spd: Number.isFinite(slot.spd) && slot.spd > 0 ? slot.spd : null,
    })
    .filter(Boolean);
  if (slots.length === 0) return null;
  return {
    name: draft.name.trim() || "Untitled team",
    gameMode: draft.gameMode || null,
    notes: draft.notes.trim() || null,
    slots,
  };
};

/** Turns a saved team (GET /teams/{code}) back into a draft, to edit a copy of it. */
export const draftFromTeam = (team) => {
  const draft = emptyDraft();
  draft.name = team.name ?? "";
  draft.gameMode = team.gameMode ?? null;
  draft.notes = team.notes ?? "";
  for (const slot of team.slots ?? []) {
    if (slot.position >= 0 && slot.position < TEAM_SIZE) {
      draft.slots[slot.position] = {
        characterId: slot.character.id,
        eidolon: slot.eidolon ?? 0,
        lightConeId: slot.lightCone?.id ?? null,
        superimposition: slot.superimposition ?? 1,
        spd: slot.spd ?? null,
      };
    }
  }
  return draft;
};

/**
 * Why a character can't go into slot `position` (null = it can).
 * Same rules the backend enforces as errors: no duplicates, one version per variant group.
 */
export const pickBlocker = (char, draft, position, charactersById) => {
  for (const [index, slot] of draft.slots.entries()) {
    if (!slot || index === position) continue;
    if (slot.characterId === char.id) return "Already in the team";
    const other = charactersById[slot.characterId];
    if (char.variantGroup && other?.variantGroup === char.variantGroup) {
      return `Same character as ${other.name}`;
    }
  }
  return null;
};

/**
 * Partners recommended for the current team: sums each candidate's synergy with every member.
 * synergiesByMember: { [memberId]: [{ partner, score }] } from GET /characters/{id}/synergies.
 * Returns [{ id, score }] best first, without current members.
 */
export const suggestPartners = (memberIds, synergiesByMember) => {
  const members = new Set(memberIds);
  const totals = new Map();
  for (const memberId of memberIds) {
    for (const { partner, score } of synergiesByMember[memberId] ?? []) {
      if (members.has(partner.id)) continue;
      totals.set(partner.id, (totals.get(partner.id) ?? 0) + score);
    }
  }
  return [...totals.entries()]
    .map(([id, score]) => ({ id, score }))
    .sort((a, b) => b.score - a.score || a.id - b.id);
};

/** Light cones of the character's Path first, then by rarity and name. */
export const sortLightconesFor = (lightcones, path) =>
  [...lightcones].sort((a, b) =>
    Number(b.path === path) - Number(a.path === path) ||
    b.rarity - a.rarity ||
    a.name.localeCompare(b.name));
