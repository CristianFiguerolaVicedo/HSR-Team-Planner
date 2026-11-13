// Links to the sister project, HSRWiki (https://github.com/CristianFiguerolaVicedo/HSRWiki-frontend).
// Both apps use the game's own character ids, so a character page link is exact.

const DEFAULT_WIKI_URL = "https://hsr-wiki-frontend.vercel.app";

/** Base URL of HSRWiki, without trailing slash. VITE_WIKI_URL overrides the deployed site. */
export const wikiUrl = (env = import.meta.env) => (env?.VITE_WIKI_URL || DEFAULT_WIKI_URL).replace(/\/+$/, "");

/** HSRWiki page of one character: /char/{id}. */
export const wikiCharacterUrl = (id, env) => `${wikiUrl(env)}/char/${encodeURIComponent(id)}`;
