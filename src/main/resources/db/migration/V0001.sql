CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    token_version int DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE password_reset_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid references users(id),
    token_hash text NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    expires_at timestamptz NOT NULL,
    used boolean default false
);

CREATE TABLE refresh_tokens (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid REFERENCES users(id) ON DELETE CASCADE,
    jti text NOT NULL UNIQUE,
    token_hash text NOT NULL,
    device_info text,
    device_fingerprint text,
    issued_at timestamptz NOT NULL DEFAULT now(),
    expires_at timestamptz NOT NULL,
    revoked boolean NOT NULL DEFAULT false,
    replaced_by_jti text REFERENCES refresh_tokens(jti),
    last_used_at timestamptz NULL
);


CREATE INDEX ON refresh_tokens(user_id);
CREATE INDEX ON refresh_tokens(jti);
CREATE INDEX ON password_reset_tokens(user_id);
CREATE UNIQUE INDEX ON password_reset_tokens(user_id) WHERE used = false;
CREATE INDEX ON refresh_tokens((expires_at));
