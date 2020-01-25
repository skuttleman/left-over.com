DROP TABLE shows;

DROP TABLE locations;

ALTER TABLE users DROP CONSTRAINT users_valid_email;

DROP INDEX users_email;

DROP TABLE users;

DROP EXTENSION "uuid-ossp";
