# Usage History

Doomly keeps up to 365 calendar days of daily Reel counts locally. The Today
and Profile charts still show the last 30 days. Days lost before this change
cannot be reconstructed.

Signing in enables backup of the display name, goal, totals, streak and daily
counts. Each sync backfills the retained dates, not just today. The scheduler
coalesces changes into a 30-second window without resetting that window for
every Reel. Offline changes retry on subsequent activity or app entry.

No ad SDK, advertising ID, contacts, location, screen text, Instagram account
details or Reel content is added by this change. The public League uses the
display name and today's count. Signed-out counts remain local.

## Database Contract

The existing `daily_history` table needs a unique constraint or index on
`(uid, date)` for the batch upsert. Apply
`supabase/migrations/20261112_daily_history_upsert.sql` if the database does not
already have one. This migration does not delete duplicate rows; investigate
any duplicates before attempting migration.

Existing row-level security must limit writes to `auth.uid() = uid`. Do not put
a service-role key in the app or relax write policies to make backups work.
This change does not deploy migrations automatically or delete old cloud rows;
the 365-day limit bounds the local backup window, not server-side retention.

## Verification

- While signed out, counts should work without uploading history.
- While signed in, count Reels continuously for over 30 seconds and verify a
  history sync occurs without needing to stop scrolling.
- Go offline across a day boundary, then reconnect and open Doomly. Verify both
  dates are present in `daily_history`.
- On another installation, sign in and verify today's restored count and prior
  dates. A lower restored count must not reduce a higher local count.
- Check onboarding on a small screen, landscape, large font and with the
  number keyboard open: content scrolls while the action stays reachable.
