import { test } from "node:test";
import assert from "node:assert/strict";
import { EMPTY_FILTERS, activeFilterCount, filterCharacters, toggleIn } from "../filters.js";

const seele = { id: 1102, name: "Seele", element: "QUANTUM", path: "HUNT", role: "MAIN_DPS", rarity: 5 };
const sparkle = { id: 1306, name: "Sparkle", element: "QUANTUM", path: "HARMONY", role: "AMPLIFIER", rarity: 5 };
const pela = { id: 1106, name: "Pela", element: "ICE", path: "NIHILITY", role: "AMPLIFIER", rarity: 4 };
const dhil = { id: 1213, name: "Dan Heng • Imbibitor Lunae", element: "IMAGINARY", path: "DESTRUCTION", role: "MAIN_DPS", rarity: 5 };
const all = [seele, sparkle, pela, dhil];
const names = (list) => list.map((c) => c.name);

test("no filters returns everyone", () => {
  assert.equal(filterCharacters(all, EMPTY_FILTERS, {}).length, 4);
});

test("options in one group are OR", () => {
  const result = filterCharacters(all, { ...EMPTY_FILTERS, elements: ["ICE", "IMAGINARY"] }, {});
  assert.deepEqual(names(result), ["Pela", "Dan Heng • Imbibitor Lunae"]);
});

test("different groups are AND", () => {
  const result = filterCharacters(all, { ...EMPTY_FILTERS, elements: ["QUANTUM"], roles: ["AMPLIFIER"] }, {});
  assert.deepEqual(names(result), ["Sparkle"]);
});

test("search ignores case, spaces and symbols", () => {
  const result = filterCharacters(all, { ...EMPTY_FILTERS, search: "danheng imbibitor" }, {});
  assert.deepEqual(names(result), ["Dan Heng • Imbibitor Lunae"]);
});

test("ownership filter uses the roster", () => {
  const owned = { 1102: 0, 1106: 6 };
  assert.deepEqual(names(filterCharacters(all, { ...EMPTY_FILTERS, ownership: "owned" }, owned)), ["Seele", "Pela"]);
  assert.deepEqual(names(filterCharacters(all, { ...EMPTY_FILTERS, ownership: "missing" }, owned)), ["Sparkle", "Dan Heng • Imbibitor Lunae"]);
});

test("activeFilterCount counts every active choice", () => {
  assert.equal(activeFilterCount(EMPTY_FILTERS), 0);
  assert.equal(activeFilterCount({ ...EMPTY_FILTERS, elements: ["ICE", "FIRE"], search: "se", ownership: "owned" }), 4);
});

test("toggleIn adds and removes", () => {
  assert.deepEqual(toggleIn(["ICE"], "FIRE"), ["ICE", "FIRE"]);
  assert.deepEqual(toggleIn(["ICE", "FIRE"], "ICE"), ["FIRE"]);
});

test("a character with several roles matches any of them", () => {
  const ashveil = { id: 1999, name: "Ashveil", element: "LIGHTNING", path: "HUNT", role: "SUB_DPS", roles: ["SUB_DPS", "DEBUFFER"], rarity: 5 };
  const list = [...all, ashveil];
  assert.deepEqual(names(filterCharacters(list, { ...EMPTY_FILTERS, roles: ["DEBUFFER"] }, {})), ["Ashveil"]);
  assert.deepEqual(names(filterCharacters(list, { ...EMPTY_FILTERS, roles: ["SUB_DPS"] }, {})), ["Ashveil"]);
});

test("damage tags filter with OR, and untagged characters drop out", () => {
  const herta = { id: 1013, name: "Herta", element: "ICE", path: "ERUDITION", roles: ["MAIN_DPS"], tags: ["AOE"], rarity: 4 };
  const firefly = { id: 1310, name: "Firefly", element: "FIRE", path: "DESTRUCTION", roles: ["MAIN_DPS"], tags: ["AOE", "BREAK"], rarity: 5 };
  const list = [...all, herta, firefly];
  assert.deepEqual(names(filterCharacters(list, { ...EMPTY_FILTERS, tags: ["BREAK"] }, {})), ["Firefly"]);
  assert.deepEqual(names(filterCharacters(list, { ...EMPTY_FILTERS, tags: ["AOE"] }, {})), ["Herta", "Firefly"]);
  assert.equal(activeFilterCount({ ...EMPTY_FILTERS, tags: ["AOE", "BREAK"] }), 2);
});
