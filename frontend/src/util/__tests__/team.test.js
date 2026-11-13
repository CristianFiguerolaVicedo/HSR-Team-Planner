import { test } from "node:test";
import assert from "node:assert/strict";
import {
  draftFromTeam, emptyDraft, newSlot, pickBlocker, slotsFromRecommendation, sortLightconesFor, suggestPartners, toTeamRequest,
} from "../team.js";

const byId = {
  1102: { id: 1102, name: "Seele", variantGroup: null },
  8005: { id: 8005, name: "Trailblazer (Harmony) · M", variantGroup: "TRAILBLAZER" },
  8006: { id: 8006, name: "Trailblazer (Harmony) · F", variantGroup: "TRAILBLAZER" },
  8002: { id: 8002, name: "Trailblazer (Destruction) · F", variantGroup: "TRAILBLAZER" },
};

test("an empty draft sends nothing", () => {
  assert.equal(toTeamRequest(emptyDraft()), null);
});

test("toTeamRequest keeps positions and cleans the slots", () => {
  const draft = emptyDraft();
  draft.name = "  Seele hypercarry ";
  draft.slots[2] = { ...newSlot(1102, 2), spd: 134.5 };
  draft.slots[0] = { ...newSlot(1306), lightConeId: 7, superimposition: 3, spd: NaN };

  const request = toTeamRequest(draft);

  assert.equal(request.name, "Seele hypercarry");
  assert.equal(request.notes, null);
  assert.deepEqual(request.slots, [
    { position: 0, characterId: 1306, lightConeId: 7, eidolon: 0, superimposition: 3, spd: null },
    { position: 2, characterId: 1102, lightConeId: null, eidolon: 2, superimposition: null, spd: 134.5 },
  ]);
});

test("a team without a name still gets one", () => {
  const draft = emptyDraft();
  draft.slots[0] = newSlot(1102);
  assert.equal(toTeamRequest(draft).name, "Untitled team");
});

test("draftFromTeam rebuilds the slots in their positions", () => {
  const draft = draftFromTeam({
    name: "Shared", gameMode: "PURE_FICTION", notes: null,
    slots: [{ position: 3, character: { id: 1102 }, lightCone: { id: 9 }, eidolon: 1, superimposition: 5, spd: 160 }],
  });
  assert.equal(draft.gameMode, "PURE_FICTION");
  assert.equal(draft.notes, "");
  assert.deepEqual(draft.slots, [null, null, null,
    { characterId: 1102, eidolon: 1, lightConeId: 9, superimposition: 5, spd: 160 }]);
});

test("pickBlocker stops duplicates and two versions of the same character", () => {
  const draft = emptyDraft();
  draft.slots[0] = newSlot(1102);
  draft.slots[1] = newSlot(8005);

  assert.equal(pickBlocker(byId[1102], draft, 2, byId), "Already in the team");
  assert.match(pickBlocker(byId[8006], draft, 2, byId), /Same character/);
  assert.equal(pickBlocker(byId[8002], draft, 2, byId) !== null, true);
});

test("pickBlocker allows replacing a character in its own slot", () => {
  const draft = emptyDraft();
  draft.slots[1] = newSlot(8005);
  assert.equal(pickBlocker(byId[8006], draft, 1, byId), null);
  assert.equal(pickBlocker(byId[8005], draft, 1, byId), null);
});

test("suggestPartners adds up synergies with every member and skips members", () => {
  const synergies = {
    1102: [{ partner: { id: 1306 }, score: 6 }, { partner: { id: 1006 }, score: 2 }, { partner: { id: 1208 }, score: 1 }],
    1306: [{ partner: { id: 1102 }, score: 6 }, { partner: { id: 1208 }, score: 4 }],
  };
  assert.deepEqual(suggestPartners([1102, 1306], synergies), [{ id: 1208, score: 5 }, { id: 1006, score: 2 }]);
});

test("light cones of the character's path come first", () => {
  const list = [
    { id: 1, name: "B", rarity: 4, path: "HUNT" },
    { id: 2, name: "A", rarity: 5, path: "HARMONY" },
    { id: 3, name: "C", rarity: 5, path: "HUNT" },
    { id: 4, name: "A", rarity: 4, path: "HUNT" },
  ];
  assert.deepEqual(sortLightconesFor(list, "HUNT").map((lc) => lc.id), [3, 4, 1, 2]);
});

test("a recommendation fills the slots and keeps builds already in the draft", () => {
  const draft = emptyDraft();
  draft.slots[2] = { ...newSlot(1306, 1), lightConeId: 7, superimposition: 2, spd: 160 };
  const team = { members: [{ id: 1102 }, { id: 1306 }, { id: 1106 }, { id: 1208 }], eidolons: [2, 0, 6, 0] };

  const slots = slotsFromRecommendation(team, draft);

  assert.deepEqual(slots[0], newSlot(1102, 2));
  assert.deepEqual(slots[1], { characterId: 1306, eidolon: 1, lightConeId: 7, superimposition: 2, spd: 160 });
  assert.deepEqual(slots[2], newSlot(1106, 6));
  assert.equal(slots.length, 4);
});
