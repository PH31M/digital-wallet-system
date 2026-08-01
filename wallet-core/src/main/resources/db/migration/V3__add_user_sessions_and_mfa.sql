alter table users
    add column mfa_enabled boolean not null default false;

create table user_sessions (
    id uuid primary key,
    created_at timestamptz not null default now(),
    user_id uuid not null,
    refresh_token_id varchar(64) not null,
    device_name varchar(255),
    ip_address varchar(255),
    user_agent text,
    last_used_at timestamptz not null default now(),
    expires_at timestamptz not null,
    revoked_at timestamptz,
    constraint fk_user_sessions_user foreign key (user_id) references users(id) on delete cascade,
    constraint uq_user_sessions_refresh_token_id unique (refresh_token_id)
);

create index idx_user_sessions_user_id on user_sessions(user_id);
create index idx_user_sessions_active on user_sessions(user_id, revoked_at, expires_at);