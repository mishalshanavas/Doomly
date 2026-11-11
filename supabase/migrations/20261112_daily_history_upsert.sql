-- Backfills use ON CONFLICT (uid, date): keep one count per user and calendar day.
-- If legacy duplicate rows exist, this fails rather than silently deleting history.
create unique index if not exists daily_history_uid_date_key
    on public.daily_history (uid, date);
