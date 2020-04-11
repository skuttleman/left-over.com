ALTER TABLE shows
    ALTER COLUMN location_id DROP NOT NULL,
    ALTER COLUMN name DROP NOT NULL,
    ALTER COLUMN timezone DROP NOT NULL,
    ALTER COLUMN timezone DROP DEFAULT,
    ALTER COLUMN date_time DROP NOT NULL,
    ADD COLUMN confirmed BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN event_id VARCHAR(512),
    ADD COLUMN temp_data JSONB,
    ADD CONSTRAINT shows_location_id_or_unconfirmed CHECK ((location_id IS NOT NULL) OR (NOT confirmed)),
    ADD CONSTRAINT shows_name_or_unconfirmed CHECK ((name IS NOT NULL) OR (NOT confirmed)),
    ADD CONSTRAINT shows_timezone_or_unconfirmed CHECK ((timezone IS NOT NULL) OR (NOT confirmed)),
    ADD CONSTRAINT shows_date_time_or_unconfirmed CHECK ((date_time IS NOT NULL) OR (NOT confirmed));

CREATE UNIQUE INDEX shows_event_id ON shows (event_id);

UPDATE shows
SET confirmed = true;
