-- Teams saved while logged in have an owner, who can edit them (same link) or delete them.
-- Teams saved without an account keep owner_id NULL: they stay immutable, as before.
-- If the owner's account goes away the team stays reachable by its link, without an owner.
ALTER TABLE teams ADD COLUMN owner_id BIGINT REFERENCES users (id) ON DELETE SET NULL;
ALTER TABLE teams ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE;

CREATE INDEX teams_owner_idx ON teams (owner_id);
