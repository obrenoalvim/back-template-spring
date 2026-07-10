ALTER TABLE users
    ADD COLUMN password_hash VARCHAR(255) NOT NULL DEFAULT '',
    ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN verification_token VARCHAR(255),
    ADD COLUMN verification_token_expires_at TIMESTAMPTZ,
    ADD COLUMN reset_token VARCHAR(255),
    ADD COLUMN reset_token_expires_at TIMESTAMPTZ;

ALTER TABLE users ALTER COLUMN password_hash DROP DEFAULT;
