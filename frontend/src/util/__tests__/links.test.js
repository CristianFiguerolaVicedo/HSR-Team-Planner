import { test } from "node:test";
import assert from "node:assert/strict";
import { wikiCharacterUrl, wikiUrl } from "../links.js";
import { addCharacterToDraft, emptyDraft, newSlot } from "../team.js";

test("wiki links default to the deployed HSRWiki and can be overridden", () => {
  assert.equal(wikiUrl({}), "https://hsr-wiki-frontend.vercel.app");
  assert.equal(wikiUrl({ VITE_WIKI_URL: "http://localhost:5174/" }), "http://localhost:5174");
  assert.equal(wikiCharacterUrl(1102, {}), "https://hsr-wiki-frontend.vercel.app/char/1102");
});

test("?with= puts the character in the first free slot", () => {
  const draft = emptyDraft();
  draft.slots[0] = newSlot(1306);
  const result = addCharacterToDraft(draft, "1102", 2);
  assert.deepEqual(result.slots[1], newSlot(1102, 2));
  assert.equal(result.slots[0].characterId, 1306);
});

test("?with= changes nothing for duplicates, a full team or a bad id", () => {
  const draft = emptyDraft();
  draft.slots[0] = newSlot(1102);
  assert.equal(addCharacterToDraft(draft, 1102), draft);
  assert.equal(addCharacterToDraft(draft, "abc"), draft);
  assert.equal(addCharacterToDraft(draft, "-4"), draft);
  const full = { ...emptyDraft(), slots: [newSlot(1), newSlot(2), newSlot(3), newSlot(4)] };
  assert.equal(addCharacterToDraft(full, 1102), full);
});
