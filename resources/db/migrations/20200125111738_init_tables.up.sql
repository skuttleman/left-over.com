CREATE EXTENSION "uuid-ossp";

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    external_id VARCHAR(255) UNIQUE NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now()
);

ALTER TABLE users ADD CONSTRAINT users_valid_email CHECK (email ~ '^[a-z\-\+_0-9\.]+@[a-z\-\+_0-9]+\.[a-z\-\+_0-9\.]+$');

CREATE UNIQUE INDEX users_email ON users (email);

CREATE TABLE locations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) UNIQUE NOT NULL,
    city VARCHAR(255) NOT NULL,
    state VARCHAR(2) NOT NULL,
    website VARCHAR(255) UNIQUE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    created_by UUID REFERENCES users NOT NULL
);

CREATE TABLE shows (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    location_id UUID NOT NULL REFERENCES locations,
    name VARCHAR(255) NOT NULL,
    timezone VARCHAR(255) NOT NULL DEFAULT 'America/New_York',
    date_time TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    created_by UUID NOT NULL REFERENCES users
);
