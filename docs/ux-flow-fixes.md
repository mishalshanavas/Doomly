# UX flow fixes

## Changes

1. Setup asks for a user-selected daily goal, then offers optional tracking and
   optional notifications. An account is no longer required to enter the app.
   Users with existing recorded activity retain their current goal and access.
2. Disabling Accessibility no longer redirects to onboarding. Today displays a
   tracking-paused repair card, with the full disclosure before opening Settings.
3. UI celebrations, milestone labels, motivational copy, and notifications share
   MotivationEngine rules: 25-Reel boundaries plus the selected daily goal.
4. Notifications replace the previous notification in their category. Regular
   celebrations have a five-minute cooldown and daily cap of four; the first
   goal celebration can bypass the cooldown/cap. Android independently controls
   milestone and update channels. Profile links directly to those controls.
   Tapping milestones opens Today; tapping updates opens Profile. Legacy muted
   milestone-channel importance is preserved during migration.
5. Counts and goal edits save locally first. Automatic synchronization continues
   when signed in, without sync labels or controls in the simplified UI. Goal
   edits confirm the local save without waiting for the network.
6. Streaks expire after a missed day, before another Reel is recorded. The daily
   record includes today's count. Visible stats refresh on broadcasts and midnight.
7. League supports optional sign-in, retry, refresh, and explicit stale-ranking
   labels. Rankings are cleared when the account or day changes.
8. The board pawn now appears on green launcher backgrounds at all densities,
   with an adaptive safe-zone foreground, themed monochrome layer, matching
   status-bar silhouette, and updated store artwork. Regenerate with
   `python scripts/generate_launcher.py` (requires Pillow and CairoSVG).

## Validation

Both `assembleGithubDebug` and `assemblePlayDebug` build successfully.
`testGithubDebugUnitTest` passes 27 tests, including shared milestone thresholds,
missed-day streak display, cooldown, daily notification cap, and clock reversal.
`lintGithubDebug` completes with no errors; warnings remain in the project.

## Backend rollout

`supabase/functions/admin-notify/index.ts` now sends data messages so Android's
notification helper handles both foreground and background delivery consistently.
Deploy the updated `admin-notify` function alongside the app release. Older
notification payloads retain a compatibility fallback, but background payloads
from an older server may still use Firebase's automatic notification behavior.

## Device verification

Exercise fresh install, numeric goal validation, skipping each optional permission,
notification denial followed by Settings recovery, Accessibility disable/re-enable,
local-only league/profile, offline goal save, cloud retry, cold/warm notification
routing, channel muting, midnight rollover, and large font settings.
Cloud-authenticated scenarios require a test account; do not use production users
or send real push notifications just to verify the UI.

Emulator verification was attempted on this host but could not complete: the
Android guest CPU threads hang under the host's KVM configuration. Device checks
above remain pending. No real push notifications were sent and no backend was
deployed during validation.
