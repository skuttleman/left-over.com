ALTER TABLE locations
    DROP CONSTRAINT locations_name_key,
    DROP CONSTRAINT locations_website_key;

CREATE UNIQUE INDEX locations_name_city_state on locations (name, city, state);
