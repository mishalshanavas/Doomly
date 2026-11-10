create table if not exists public.device_tokens (
    uid uuid not null references auth.users(id) on delete cascade,
    token text primary key,
    platform text not null default 'android' check (platform in ('android')),
    updated_at timestamptz not null default now()
);

alter table public.device_tokens enable row level security;

create policy "users can manage their own device tokens"
    on public.device_tokens for all
    using (auth.uid() = uid)
    with check (auth.uid() = uid);

create index if not exists device_tokens_uid_idx on public.device_tokens(uid);
