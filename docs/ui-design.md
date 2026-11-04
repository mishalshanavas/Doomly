# Doomly UI

The palette is white, near-black, green, and restrained pastel fills, inspired by
the app preview at https://myturn.kr/ (inspected November 4, 2026). Doomly uses its
own text, data, local geometric avatars, and layouts.

- Home places the streak next to the date/title. The week strip and a 2x2 grid
  show today's Reels, the editable goal, next milestone, and level without accordions.
  The Reels action stays prominent. No pawn mascot appears on Home.
- Profile shows a deterministic SHA-256-based mirrored avatar, personal statistics,
  and 30 days of activity directly, without a Usage history heading or disclosure.
  Signed-in accounts use their UID; local profiles use a persistent random seed.
- League places the top three on a 2–1–3 podium with real names, avatars, and counts;
  other players follow in an open list. Empty, local-only, retry, and stale states remain.
- Navigation uses an inset rounded dock, green selected tab, visible labels,
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
