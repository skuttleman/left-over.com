DROP INDEX shows_event_id;

ALTER TABLE shows
    DROP CONSTRAINT shows_location_id_or_unconfirmed,
    DROP CONSTRAINT shows_name_or_unconfirmed,
    DROP CONSTRAINT shows_timezone_or_unconfirmed,
    DROP CONSTRAINT shows_date_time_or_unconfirmed,
    DROP COLUMN temp_data,
    DROP COLUMN event_id,
    DROP COLUMN confirmed,
    ALTER COLUMN location_id SET NOT NULL,
    ALTER COLUMN name SET NOT NULL,
    ALTER COLUMN timezone SET NOT NULL,
    ALTER COLUMN timezone SET DEFAULT 'America/New_York',
    ALTER COLUMN date_time SET NOT NULL;
