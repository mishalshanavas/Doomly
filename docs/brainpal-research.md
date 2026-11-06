# Product direction: Doomly

Research date: 2026-11-06. Primary sources: [BrainPal website](https://brainpalapp.ai/) and [official Play listing](https://play.google.com/store/apps/details?id=com.brainrot.android). This is a public product review, not a hands-on review of the installed competitor app.

BrainPal's stated mechanics are a live short-video counter, a brain visual that deteriorates as counts increase, lower-count-wins friend battles, and paid blocking/limits. Its published scope includes several social apps. Those are product claims, not evidence of biological effects.

Doomly's interpretation of the opposite is a playful scrolling scorekeeper:

| Area | Doomly behavior |
| --- | --- |
| Daily visual | Original Mochi eyes gain levels and a crown from recorded counts. |
| Progression | Five daily levels at 0, 25, 75, 150, and 300 Reels. Resets with the local day. Cosmetic, no health claims. |
| Competition | Existing daily league orders counts descending. The UI explains the rule and shows all returned entries. |
| Target | A milestone to surpass, with continuous counting beyond the target. |
| Rewards | Existing rotating daily achievement and optional 10-Reel session celebrations. |
| Sharing | Native Android share sheet sends the user's actual daily score on request. |
| Scope | Instagram Android Reels only. No claim to track other apps or browser usage. |
| Control | Optional tracking, optional notifications and overlay, no blocking, no required account for local use. |

## Reliability changes

- Switching update channels invalidates outstanding callbacks and clears pending installation state.
- Release selection skips drafts and releases without a valid APK, then chooses the highest newer build code among the returned releases. Legacy release versions still work without build metadata.
- Build-code comparison handles returning from a beta to a newer stable version without a downgrade.
- Beta CI now runs lint and unit tests before producing an APK. Existing signing/package/version verification is retained.
- Local target saves remain successful when cloud sync fails; the UI reports the sync issue and schedules a retry.
- Sign-in copy accurately explains that name and aggregate scores are shared for the league.

## Release verification

Run `bash gradlew :app:testGithubDebugUnitTest :app:lintGithubDebug :app:assembleGithubDebug :app:assemblePlayDebug`.

Physical-device acceptance is still required: Instagram counting on current app versions; accessibility interruption/re-enable; overnight reset; large-font and small-screen layouts; Google sign-in and live rankings; overlay placement on OEM cutouts; update install with the production certificate; and beta-to-stable migration. This workspace has no connected Android device. Passing build checks alone does not certify those flows or Play distribution readiness.

Validation in this workspace: 36 unit tests passed, both debug variants packaged, and GitHub debug lint completed with 0 errors and 98 warnings. Lint warnings remain; the app is not certified production-ready. No live backend, physical-device, or production signing-key installation tests were performed.

## Temporary APK publishing

Use `bash scripts/push-with-apk.sh [path/to/local.apk]` after committing. It pushes master, then stages an available APK in a prerelease only if the file is at least as recent as the commit, has the correct package, is signed like the latest stable APK, and has a lower version code than the pending CI replacement. An unavailable, stale, or differently signed local APK is not published. This is a freshness check, not a proof that a local APK was built from that exact commit.

The helper and beta workflow derive the same tag from the commit timestamp and use the same asset filename. CI overwrites that asset and release notes after its signed build passes. CI uses the next version code so users who installed the temporary build can upgrade. An existing release is never overwritten by the local helper. Ordinary `git push` cannot upload an APK from the developer's filesystem; it only triggers GitHub CI. Tagged stable releases continue to use the separate stable workflow.
