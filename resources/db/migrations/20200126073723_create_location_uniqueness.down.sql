DROP INDEX locations_name_city_state;

ALTER TABLE locations
    ADD CONSTRAINT locations_name_key UNIQUE (name),
    ADD CONSTRAINT locations_website_key UNIQUE (website);
