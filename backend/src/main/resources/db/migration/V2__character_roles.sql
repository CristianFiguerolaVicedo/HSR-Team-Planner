-- A character can now have several roles (Ashveil: Sub-DPS and Debuffer), main role first.
-- The old single role becomes the first entry; the next catalog sync applies overrides.json.

CREATE TABLE character_roles (
    character_id  INTEGER     NOT NULL REFERENCES characters (id) ON DELETE CASCADE,
    role_order    INTEGER     NOT NULL,
    role          VARCHAR(20) NOT NULL,
    PRIMARY KEY (character_id, role_order)
);

INSERT INTO character_roles (character_id, role_order, role)
SELECT id, 0, role FROM characters;

ALTER TABLE characters DROP COLUMN role;
