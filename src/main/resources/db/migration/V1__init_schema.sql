-- V1__init_user_schema.sql

-- Role enum
CREATE TYPE user_role AS ENUM ('HOST', 'GUEST');

-- Users table
CREATE TABLE users (
    id UUID PRIMARY KEY,

    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL, -- BCrypt hash
    email VARCHAR(255) NOT NULL,

    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    residence VARCHAR(150) NOT NULL,

    role user_role NOT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
