# HSR Team Planner

**Plan Honkai: Star Rail teams with the characters you own, check SPD breakpoints and share builds with a link.**

![Java](https://img.shields.io/badge/Java-21-1F2A44?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0-6DB33F?logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-4169E1?logo=postgresql&logoColor=white)
![React](https://img.shields.io/badge/React-19-61DAFB?logo=react&logoColor=black)
![Tailwind CSS](https://img.shields.io/badge/Tailwind%20CSS-4-06B6D4?logo=tailwindcss&logoColor=white)

> Status: **MVP complete, phase 2 in progress**. Done: catalog, "My characters", team builder with live validation, SPD calculator, share links and a rule-based team recommender, import by UID. Next: Discord/Google login, deployment.

## What it does

1. **Catalog** of characters and light cones, synced from my [HSRWiki API](https://github.com/CristianFiguerolaVicedo/HSRWiki-backend).
2. **My characters**: mark the ones you own (stored in the browser).
3. **Team builder**: 4 slots with live checks for game rules, roles and synergies.
4. **SPD calculator**: actions in cycle 0 and the SPD missing for the next breakpoint.
5. **Share by link**: every saved team gets a short id, e.g. `/t/aZ3kP9qX`.

Phase 2: import characters by UID (Mihomo / Enka.Network), Discord/Google login, and a rule-based recommender per game mode.

## Project structure

```
HSR-Team-Planner/
├── backend/                         Spring Boot API (Java 21)
│   ├── src/main/java/com/crifigvic/hsrplanner/
│   │   ├── catalog/                 Character, LightCone, Synergy + read endpoints
│   │   ├── sync/                    HSRWiki client, catalog sync, synergy extraction
│   │   ├── team/                    Team, TeamSlot, share codes, endpoints
│   │   │   └── validation/          Team rules (pure Java, no Spring)
│   │   ├── spd/                     SPD calculator (pure Java) + endpoint
│   │   ├── recommend/               Rule-based team recommender (pure Java) + endpoint
│   │   ├── importer/                Import by UID from Enka.Network / Mihomo
│   │   ├── config/                  Properties, CORS, clock, scheduling
│   │   └── common/                  Error handling (RFC 9457 problem details)
│   ├── src/main/resources/
│   │   ├── db/migration/            Flyway migrations
│   │   └── catalog/overrides.json   Manual roles, aliases and variant groups
│   └── src/test/java/...            JUnit 5 tests
├── frontend/                        React + Vite + Tailwind CSS (same setup as HSRWiki-frontend)
│   └── src/
│       ├── pages/                   One page per route (lazy-loaded)
│       ├── components/              Cards, filters, character detail, layout
│       ├── hooks/                   Data (characters, light cones, teams), roster and draft stores, debounced checks
│       ├── services/                axios client with retries + one function per endpoint
│       └── util/                    Pure logic (filters, roster, game labels) + node --test tests
└── docker-compose.yml               Local PostgreSQL
```

The backend is organised **by feature**, not by layer: each folder holds its entity, repository, service and controller. The rules that matter most (SPD maths and team validation) are plain Java classes with no Spring or JPA, so their tests run in milliseconds without a database.

## Data model

```
Character ──< Synergy >── Character        TeamSlot >── Character
    (game id)    (pair, score, source)     TeamSlot >── LightCone (optional)
                                           Team ──< TeamSlot (max 4, one per position)
LightCone
```

| Entity | Key fields | Notes |
|---|---|---|
| `Character` | `id` (game id), `name`, `rarity`, `element`, `path`, `roles`, `baseSpd`, `variantGroup` | `roles` = Main DPS, Sub-DPS, Amplifier, Debuffer, Sustain; main role first, some characters have two. `tags` = damage profile (AoE, Single target, Break). `variantGroup` stops two versions of one character in the same team (all Trailblazers). |
| `LightCone` | `id`, `name` (unique), `rarity`, `path`, `hsrwikiId` | Matched by name: HSRWiki builds light cone ids from a name hash. |
| `Synergy` | `characterA`, `characterB`, `score`, `source`, `reason` | One row per pair with `A.id < B.id`. `BUILD_GUIDE` rows are rebuilt on every sync; `CURATED` rows are never touched. |
| `Team` | `shareCode` (8 chars, unique), `name`, `gameMode`, `notes`, `createdAt` | Immutable snapshot (no login yet): editing creates a new team. |
| `TeamSlot` | `position` (0–3), `character`, `lightCone`, `eidolon`, `superimposition`, `spd` | `spd` is the final in-combat SPD and drives the SPD analysis. |

## API

All endpoints are under `/api`. Errors are `application/problem+json`.

| Method | Endpoint | Description |
|---|---|---|
| GET | `/characters?path=&element=&role=&rarity=` | Characters, every filter optional |
| GET | `/characters/{id}` | One character |
| GET | `/characters/{id}/synergies?limit=10` | Best partners, highest score first |
| GET | `/lightcones?path=&rarity=` | Light cones |
| GET | `/lightcones/{id}` | One light cone |
| GET | `/spd?speed=134.2&cycles=3` | SPD analysis for one unit |
| POST | `/spd/team` | SPD analysis for up to 4 units: `{"speeds": [134, 160], "cycles": 3}` |
| POST | `/teams/validate` | Check a team without saving it (roles, synergies, issues) |
| POST | `/teams` | Save a team → `201` with its `shareCode`; `422` if it breaks a rule |
| GET | `/teams/{shareCode}` | A shared team, with SPD analysis per slot |
| POST | `/recommendations` | Best teams from your characters: `{"owned": {"1102": 2, ...}, "gameMode": "PURE_FICTION", "mustInclude": [1102]}` |
| GET | `/import/{uid}` | Characters and Eidolons in a player's in-game showcase (Enka.Network, Mihomo as fallback) |
| POST | `/admin/sync` | Re-sync the catalog now (header `X-Admin-Token`) |

Team issues have a severity: `ERROR` (the game forbids it: duplicates, more than 4, two Trailblazers), `WARNING` (light cone of another Path, no sustain, no damage dealer) and `INFO` (two or more sustains).

## SPD maths

A unit acts every `10000 / SPD` action value (AV). Cycle 0 lasts 150 AV and every later cycle 100 AV, so by the end of cycle `n` a unit has acted `floor((150 + 100·n) · SPD / 10000)` times. The next breakpoint is the SPD that adds one more action: `(actions + 1) · 10000 / (150 + 100·n)`.

That reproduces the usual breakpoints: 133.34 SPD for 2 actions in cycle 0, 120 for 3 in two cycles, 160 for 4 in two cycles, 142.86 / 171.43 for 5 / 6 in three cycles. Action advance and delay are not modelled yet.

## Team recommender

`TeamRecommender` (pure Java, JUnit-tested) builds teams only from the characters you own. Trying every 4-character combination would be millions of teams, so each damage dealer you own is an anchor and only its best candidates are combined: its 12 partners with the most synergy plus the best sustains and supports. That's a few hundred teams per anchor (about 60 ms for the whole catalog), and the best team of each anchor is kept so results show different cores.

Score = **synergy** (pairs recommended together in HSRWiki's guides) + **roles** (one sustain +4, none −6, up to two supports +2 each) + **game mode** (two damage dealers +3 in Pure Fiction / Apocalyptic Shadow; one DPS with two supports +3 in Memory of Chaos / Anomaly Arbitration; by tags: +2 per AoE damage dealer in Pure Fiction, +2 per Break character in Apocalyptic Shadow, up to two each, and +1 for a single-target carry in Apocalyptic Shadow / Memory of Chaos) + **Eidolons** (+0.5 each). Every team comes with the reasons behind its score.

## Import by UID

`GET /api/import/{uid}` reads the player's **in-game profile showcase** (only those characters, plus the support character, are public) from [Enka.Network](https://enka.network) and falls back to [Mihomo](https://api.mihomo.me) if Enka doesn't answer. Following Enka's rules, requests carry a custom User-Agent and each UID is cached for the `ttl` Enka returns (1–10 minutes). Their documented errors become clear messages: invalid UID (400), unknown player (404), game maintenance (503), too many requests (429). Nothing is stored: the frontend shows what would change and merges it into "My characters" when the user confirms.

## Catalog sync

`CatalogSyncService` downloads `/api/characters`, `/api/lightcones` and `/api/version` from HSRWiki, then writes everything in one transaction (if HSRWiki is down, nothing changes). It runs:

- at startup when the catalog is empty (`SYNC_ON_STARTUP=always|if-empty|never`),
- every Wednesday at 19:00 UTC, an hour after HSRWiki refreshes its data (`SYNC_CRON`),
- on demand with `POST /api/admin/sync`.

Synergies come from HSRWiki's recommended teams: a fixed team member adds 2 to the pair, each option of an "A / B" slot adds 1, and placeholders like "Any Sustain" are skipped. `catalog/overrides.json` holds the roles of every character and the damage tags of damage dealers and break supports, reviewed by hand (new characters get a default from their Path until they're added), maps short names used by the guides ("Topaz") and adds variant groups.

## Running locally

```bash
docker compose up -d                 # PostgreSQL on :5432

cd backend
./mvnw spring-boot:run               # API on :8080, syncs the catalog on first start
./mvnw test                          # unit tests

cd ../frontend
cp .env.example .env
npm install
npm run dev                          # http://localhost:5173
npm test                             # util tests (node --test)
npm run lint
```

## Configuration

| Variable | Default | Purpose |
|---|---|---|
| `SPRING_DATASOURCE_URL` / `_USERNAME` / `_PASSWORD` | local Docker database | PostgreSQL connection |
| `HSRWIKI_BASE_URL` | `https://hsrwiki-backend.onrender.com` | Where the catalog comes from |
| `SYNC_ON_STARTUP` | `if-empty` | `always`, `if-empty` or `never` |
| `SYNC_CRON` | `0 0 19 * * WED` | Weekly sync (UTC) |
| `ENKA_URL` / `MIHOMO_URL` | `https://enka.network` / `https://api.mihomo.me` | UID import providers |
| `ADMIN_TOKEN` | empty (admin disabled) | Token for `POST /api/admin/sync` |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://localhost:4173` | Frontend origins |
| `VITE_API_BASE_URL` (frontend) | `http://localhost:8080/api` in dev | API used by the site |

## Deployment

- **Frontend → Vercel.** Root directory `frontend`, set `VITE_API_BASE_URL`. `vercel.json` sends every route to `index.html` so share links work.
- **Backend → Railway or Fly.io** with the `backend/Dockerfile` and a managed PostgreSQL. Railway exposes the database as `postgresql://…`; Spring needs the JDBC form `jdbc:postgresql://host:port/db` in `SPRING_DATASOURCE_URL`.
