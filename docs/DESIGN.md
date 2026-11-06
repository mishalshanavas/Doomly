# Doomly — the fine art of one more Reel

Doomly is a playful scorekeeper for Instagram Reels, not a productivity dashboard.
The visual direction takes the restricted palette, expressive type, editorial
hierarchy and recurring character principle from the Cool Cat’s Behance reference.
It retains Doomly’s original Mochi illustration and uses an independent identity.

## Foundation

- Paper: #F8F3E7. Forest ink: #203D32. Secondary text: #5F665B.
- Rust accent: #A4472D. Sage surface: #E1E7D6. Warm surface: #EEE8D8.
- Fraunces Bold for editorial headings and tally numerals; platform sans for
  controls, body text and compact data. Font bundled for offline rendering,
  with its SIL Open Font License in docs/licenses/Fraunces.txt.
- Spacing: 4, 8, 12, 16, 24, 32. Screen gutters: 24. Reading width: at most 760dp.
- Small containers: 8dp corners. Feature surfaces: 12dp. No decorative shadows.
- Quiet dividers and numbered section labels replace redundant card nesting.
- Rust indicates emphasis; text labels convey status independently of color.

## Interaction

The four existing destinations remain Today, League, Research and Profile.
Selected navigation uses ink and a rust underline. Research remains immersive,
with close and controls actions; Instagram stays borderless. Original tracking,
authentication, permissions, sync, sharing, settings and update actions are retained.
Loading score data uses a dash rather than showing a false zero. Existing errors,
retry controls, permission disclosure and signed-out league state remain available.

## Verification

Build, unit tests and Android lint are checked before publication. Device review
is still required for Instagram login, WebView video, small-screen landscape and
large accessibility font sizes. This is an Android app; large window support uses
a constrained reading column, not a separate desktop website.
