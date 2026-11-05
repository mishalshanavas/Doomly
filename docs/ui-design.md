# Doomly UI

The palette is white, near-black, and sparse green accents, originally inspired by
the app preview at https://myturn.kr/ (inspected November 4, 2026). Doomly uses its
own text, data, local geometric avatars, and layouts.

- Home places a plain streak indicator next to the date/title. The week strip and a flat statistics row
  show today's Reels and the editable goal without accordions
  or colored cards. A small milestone row follows; today's count has the strongest emphasis.
  The Reels action stays prominent. No pawn mascot appears on Home.
- Profile shows a deterministic SHA-256-based mirrored avatar, personal statistics,
  and 30 days of activity directly, without a Usage history heading or disclosure.
  Signed-in accounts use their UID; local profiles use a persistent random seed.
- League places the top three on a 2–1–3 podium with real names, avatars, and counts;
  other players follow in an open list. Empty, local-only, retry, and stale states remain.
- Navigation uses a flat bar with outlined icons, visible labels, a small green selected marker,
  accessible tab semantics, and Android navigation-bar padding.
- Sync happens automatically in the background. There are no sync controls or
  sync status labels in the main UI. Local counts remain available offline.
- Optional notification and update controls are visible directly in Profile.

Motion is limited to count changes, a brief inline milestone celebration, button
presses, and tab transitions. There are no idle loops. Compose animation timing
respects Android's animator settings.

GeneratedAvatar.kt owns geometric avatars; MoodUi.kt owns shared cards, headers,
and the history heatmap. TurnBoard.kt retains only the date strip and illustration
helper. Unused legacy components, board-tile previews, resources, and ProGuard
rules have been removed. The launcher icon remains separate from the home layout.

## Visual restraint

Headings use sentence case and medium weight. Date labels appear only where they
help orient the day. Routine update status is a small row, while actionable updates
retain their controls. Profile statistics and history are unboxed. Podium steps are
shallow and neutral with a subdued green winner. Green is reserved for activity,
progress, avatars, and selection rather than full-width navigation fills.

Level progression and generic goal-chasing copy have been removed from Home.
The Instagram action is integrated into “A minor detour”: a thin conceptual route
from “The plan” to the inline “Open Instagram” destination. Pressing the action
briefly highlights the route; there is no idle animation or separate oversized CTA.

## Daily small achievement

Home replaces the raw next-milestone row with one compact daily achievement. Seven
dry-humor titles rotate deterministically by local date, with small targets of
3–10 Reels, capped by the daily goal when the achievement is first chosen. That
selection is frozen for the day. A small progress fraction becomes an earned check
and brief icon pop. Earned state persists locally and is reset for the next date;
opening the app or rotating the screen does not repeatedly replay an earned badge.
There is no additional notification, level system, or expanded achievement panel.

## Yesterday and long-session reminders

Home has a faint marker for yesterday's recorded total and a green dot for today.
No baseline is invented when yesterday has no data. Equal/zero values remain valid.

The accessibility service observes Instagram foreground windows while a session
is active. A session qualifies for a quiet Reel reminder only after at least 10
minutes of confirmed foreground observations and at least one detected Reel. A
real application switch is confirmed across two checks before notification. Short
sessions cannot accumulate; screen lock, screen off, service interruption, or a
long observation gap cancel the session. No other app's labels/content are read.

Reminders are limited to once per local day, use a separate Android channel, and
open Instagram directly through an activity PendingIntent. Returning to Instagram
cancels an outstanding reminder. Notification permission and channel muting are
respected; there are no alarms, new permissions, or global app-event subscriptions.

## App icon

The app icon is an original pair of half-closed, right-looking eyes in warm white
on charcoal. The SVG in `store-assets/doomly-icon.svg` is the source of truth;
`scripts/generate_launcher.py` generates every launcher density, the round icon,
adaptive/themed vector layers, the notification silhouette, and Play Store artwork.
Pupil cutouts stay transparent in the monochrome layer. No pawn is used in the icon.

## App-open updates

The GitHub distribution checks for a newer release whenever the activity enters
foreground, coalescing checks while a request/download is active. Available APKs
download automatically. Shared update state survives tab navigation and rotation;
a compact banner shows download progress or an install action across screens.
The installer is opened automatically once per ready release while foreground.
Android still requires confirmation and, where needed, permission to install from
this source. Returning after granting that permission resumes installation; cancel
or denial leaves a manual install action rather than repeatedly reopening prompts.
Verified cached APKs can be reused after a restart. The Play distribution keeps
GitHub self-updates disabled. The update feature does not use overlay APIs.

## Transient Reel pill

Profile → Reel celebrations opts into a charcoal pill over Instagram at each 10th
counted Reel. Android's display-over-other-apps permission is requested only when
that switch is enabled. The eyes icon, count, and one short appreciative line fade
in, remain for about three seconds, and fade out. Tap or long-press dismisses it.

Placement uses the reported top display cutout and status-bar insets, with a top
centre fallback and screen-edge clamping. Rotation hides the current pill. While
visible, a brief foreground check removes it on app exit, ads/comments, screen lock,
or permission revocation. It never takes keyboard focus or covers the whole screen.
The service removes its window and callbacks on interruption or destruction. Daily
milestones are deduplicated across service restarts; old milestones are not replayed.
Device testing is needed for manufacturer-specific cutout and overlay behavior.
