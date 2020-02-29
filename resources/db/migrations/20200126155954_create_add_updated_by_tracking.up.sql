ALTER TABLE users ADD COLUMN updated_at TIMESTAMP WITHOUT TIME ZONE;
ALTER TABLE locations ADD COLUMN updated_at TIMESTAMP WITHOUT TIME ZONE;
ALTER TABLE shows ADD COLUMN updated_at TIMESTAMP WITHOUT TIME ZONE;

UPDATE users SET updated_at = created_at;
UPDATE locations SET updated_at = created_at;
UPDATE shows SET updated_at = created_at;

ALTER TABLE users ALTER COLUMN updated_at SET NOT NULL;
ALTER TABLE locations ALTER COLUMN updated_at SET NOT NULL;
ALTER TABLE shows ALTER COLUMN updated_at SET NOT NULL;
