CREATE TABLE jars (
    id uuid primary key default gen_random_uuid(),
    title VARCHAR(20) NOT NULL,
    owner_id uuid NOT NULL references users(id),
    amount decimal(12, 2) NOT NULL DEFAULT 0.0,
    can_spend boolean NOT NULL DEFAULT true,
    can_add boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT NOW(),
    updated_at timestamptz NOT NULL DEFAULT NOW(),
    deleted_at timestamptz
);

CREATE table jar_accesses (
    jar_id uuid NOT NULL references jars(id),
    user_id uuid not null references users(id),
    can_view boolean not null default true,
    can_add boolean not null default false,
    can_spend boolean not null default false,
    created_at timestamptz not null default NOW(),
    updated_at timestamptz not null default NOW(),
    PRIMARY KEY (jar_id, user_id)
);

create table transactions (
    id uuid not null primary key default gen_random_uuid(),
    jar_id uuid not null references jars(id),
    user_id uuid not null references users(id),
    amount decimal(12, 2) not null,
    title varchar(20) not null default 'UNTITLED',
    description TEXT,
    created_at timestamptz NOT NULL default NOW(),
    updated_at timestamptz NOT NULL default NOW()
);
CREATE INDEX idx_transactions_created_at ON transactions(created_at DESC);
CREATE UNIQUE INDEX idx_users_email ON users(email);
CREATE INDEX idx_jars_owner_id ON jars(owner_id);
CREATE INDEX idx_jars_not_deleted ON jars(deleted_at) WHERE deleted_at IS NULL;
CREATE INDEX idx_jar_accesses_user_id ON jar_accesses(user_id);
CREATE INDEX idx_jar_accesses_jar_id ON jar_accesses(jar_id);
CREATE INDEX idx_transactions_jar_id_created_at ON transactions(jar_id, created_at DESC);
CREATE INDEX idx_transactions_user_id ON transactions(user_id);
CREATE INDEX idx_transactions_jar_id ON transactions(jar_id);
