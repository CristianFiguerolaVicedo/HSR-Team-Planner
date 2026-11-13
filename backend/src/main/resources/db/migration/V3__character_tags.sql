-- Damage profile tags (AOE, SINGLE_TARGET, BREAK). Filled by the next catalog sync from overrides.json.
CREATE TABLE character_tags (
    character_id  INTEGER     NOT NULL REFERENCES characters (id) ON DELETE CASCADE,
    tag           VARCHAR(20) NOT NULL,
    PRIMARY KEY (character_id, tag)
);
