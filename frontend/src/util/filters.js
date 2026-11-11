export const EMPTY_FILTERS = {
  search: "",
  elements: [],
  paths: [],
  roles: [],
  rarities: [],
  ownership: "all", // "all" | "owned" | "missing"
};

/** Ignores case, spaces and symbols: "danheng" finds "Dan Heng • Imbibitor Lunae". */
const normalize = (text) => text.toLowerCase().replace(/[^a-z0-9]/g, "");

/**
 * Inside one group the options are OR (Fire or Ice); between groups they are AND (Fire and Hunt).
 * An empty group means "any". `owned` is the roster object: { [id]: eidolon }.
 */
export const filterCharacters = (characters, filters, owned) => {
  const query = normalize(filters.search);
  return characters.filter((char) => {
    const isOwned = String(char.id) in owned;
    if (filters.ownership === "owned" && !isOwned) return false;
    if (filters.ownership === "missing" && isOwned) return false;
    if (query && !normalize(char.name).includes(query)) return false;
    if (filters.elements.length > 0 && !filters.elements.includes(char.element)) return false;
    if (filters.paths.length > 0 && !filters.paths.includes(char.path)) return false;
    if (filters.roles.length > 0 && !filters.roles.includes(char.role)) return false;
    if (filters.rarities.length > 0 && !filters.rarities.includes(char.rarity)) return false;
    return true;
  });
};

export const activeFilterCount = (filters) =>
  filters.elements.length + filters.paths.length + filters.roles.length + filters.rarities.length +
  (filters.search.trim() ? 1 : 0) + (filters.ownership !== "all" ? 1 : 0);

/** Adds the value if missing, removes it if present. */
export const toggleIn = (list, value) =>
  list.includes(value) ? list.filter((v) => v !== value) : [...list, value];
