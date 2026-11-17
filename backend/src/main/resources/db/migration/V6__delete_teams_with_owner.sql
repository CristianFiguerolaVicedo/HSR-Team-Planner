-- Deleting an account deletes its teams too (and, through team_slots, their slots):
-- "delete my account and my data" includes the names and notes the user wrote.
-- Teams saved without an account (owner_id NULL) are not affected.
ALTER TABLE teams DROP CONSTRAINT teams_owner_id_fkey;
ALTER TABLE teams ADD CONSTRAINT teams_owner_id_fkey
    FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE CASCADE;
