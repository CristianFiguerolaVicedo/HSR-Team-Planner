// Labels, icons and display order for the enums the API sends (ICE, HUNT, MAIN_DPS...)

export const ELEMENTS = [
  { name: "PHYSICAL", label: "Physical", icon: "/assets/Type_Physical.webp" },
  { name: "FIRE", label: "Fire", icon: "/assets/Type_Fire.webp" },
  { name: "ICE", label: "Ice", icon: "/assets/Type_Ice.webp" },
  { name: "LIGHTNING", label: "Lightning", icon: "/assets/Type_Lightning.webp" },
  { name: "WIND", label: "Wind", icon: "/assets/Type_Wind.webp" },
  { name: "QUANTUM", label: "Quantum", icon: "/assets/Type_Quantum.webp" },
  { name: "IMAGINARY", label: "Imaginary", icon: "/assets/Type_Imaginary.webp" },
];

export const PATHS = [
  { name: "DESTRUCTION", label: "Destruction", icon: "/assets/Icon_Destruction.webp" },
  { name: "HUNT", label: "The Hunt", icon: "/assets/Icon_The_Hunt.webp" },
  { name: "ERUDITION", label: "Erudition", icon: "/assets/Icon_Erudition.webp" },
  { name: "HARMONY", label: "Harmony", icon: "/assets/Icon_Harmony.webp" },
  { name: "NIHILITY", label: "Nihility", icon: "/assets/Icon_Nihility.webp" },
  { name: "PRESERVATION", label: "Preservation", icon: "/assets/Icon_Preservation.webp" },
  { name: "ABUNDANCE", label: "Abundance", icon: "/assets/Icon_Abundance.webp" },
  { name: "REMEMBRANCE", label: "Remembrance", icon: "/assets/Icon_Remembrance.webp" },
  { name: "ELATION", label: "Elation", icon: "/assets/Icon_Elation.png" },
];

export const ROLES = [
  { name: "MAIN_DPS", label: "Main DPS" },
  { name: "SUB_DPS", label: "Sub-DPS" },
  { name: "AMPLIFIER", label: "Amplifier" },
  { name: "DEBUFFER", label: "Debuffer" },
  { name: "SUSTAIN", label: "Sustain" },
];

/** Damage profile, set by hand on the backend (overrides.json). */
export const TAGS = [
  { name: "AOE", label: "AoE" },
  { name: "SINGLE_TARGET", label: "Single target" },
  { name: "BREAK", label: "Break" },
];

const byName = (list) => Object.fromEntries(list.map((item) => [item.name, item]));
export const ELEMENT = byName(ELEMENTS);
export const PATH = byName(PATHS);
export const ROLE = byName(ROLES);
export const TAG = byName(TAGS);

/** A character's roles (main first), falling back to the single `role` older API answers had. */
export const rolesOf = (char) => (char.roles?.length ? char.roles : [char.role].filter(Boolean));

/** "Sub-DPS / Debuffer" */
export const roleLabel = (char) => rolesOf(char).map((r) => ROLE[r]?.label ?? r).join(" / ");

/** "AoE / Break", or "" for characters without tags. */
export const tagLabel = (char) => (char.tags ?? []).map((t) => TAG[t]?.label ?? t).join(" / ");

/** Washi tape class from index.css, one colour per element (same as HSRWiki). */
export const tapeClass = (element) => `tape-${element.toLowerCase()}`;

/** Small, stable tilt per character so the board looks hand-assembled but never reshuffles (from HSRWiki). */
export const tiltFor = (id, spread) => ((Math.abs(id) % 7) - 3) * spread;
