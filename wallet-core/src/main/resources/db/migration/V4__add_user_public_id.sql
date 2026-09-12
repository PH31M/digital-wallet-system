create extension if not exists pgcrypto;

alter table users
    add column public_id uuid;

update users
set public_id = gen_random_uuid()
where public_id is null;

alter table users
    alter column public_id set not null;

alter table users
    add constraint uq_users_public_id unique (public_id);