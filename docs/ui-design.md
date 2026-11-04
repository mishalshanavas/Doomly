# Doomly board UI

Reference inspected on November 4, 2026: https://myturn.kr/, its public stylesheet,
and the app screenshot embedded in its Lottielab animation. The app screenshot,
rather than the marketing page, guides the Android interface.

- White background, near-black text and fine outlines; 7dp corner radius.
- Heavy uppercase sans-serif headings with a small date above.
- Seven outlined date markers, a second ring around today, green for recorded usage.
- Three-column tiles connected in a winding board. Pastel tile fills and outlined
  illustrations sit alongside real Doomly statistics.
- Black primary buttons and a white bottom navigation with a green active marker.

Doomly retains its own text, data, authentication, permission disclosure, goal editor,
Instagram launcher, leaderboard, profile, history, cloud sync, and update flow.
The weekly markers represent usage, not habit completion. The default theme is
light to match the reference; an explicit dark theme remains available in code.

Seven original Android vector assets live in `res/drawable/board_*.xml`: phone,
flag, clock, coffee, tree, trophy, and pawn. They require no network, bitmap scaling,
or third-party image dependency. No MyTurn logo or downloaded artwork is bundled.

Shared board primitives are in `ui/components/TurnBoard.kt`. Shared cards, status
markers, and history are in `MoodUi.kt`; palette and typography are in `ui/theme`.
All four Compose screens, the XML launch/legacy theme, and the admin page use this design language.

Validation: `bash gradlew :app:assembleGithubDebug :app:testGithubDebugUnitTest`.
Visual review should include setup, populated/empty league, profile updates,
goal editing, a small screen, and larger Android font settings.

## Progressive disclosure

The default Today screen shows one usage card, an editable daily limit, and the
Reels action. Weekly activity, streak, level and milestones are behind “More
details”. Profile history is collapsed initially. Routine update information is
collapsed; actionable update states open automatically. The league starts with
five players and offers “Show more players”. Setup presents only the next unmet
requirement and one action, while preserving the full accessibility disclosure.
Expandable sections announce their state and preserve it across recreation.
Goal entry uses a number keyboard and validates the supported range before saving.

## Playful motion

Micro-interactions use Compose animation timing, including the Android animator
scale setting. The mascot gives a short wiggle when tapped, the Reels button
compresses while pressed, updated counts pop once, navigation colors ease, and
details chevrons rotate. There are no perpetual attention-grabbing loops.

Crossing a 25-Reel boundary after the first loaded snapshot triggers one small
trophy burst and a short inline message. Initial data hydration and rotation do
not replay the celebration. Multiple crossed milestones produce a single burst.
The app uses “daily goal” language and playful encouragement to match its premise.
