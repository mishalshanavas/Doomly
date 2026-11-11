# Doomly project checks

- Android flavors are `github` (in-app GitHub updates) and `play` (no sideload updater).
- Run unit tests with `./gradlew testGithubDebugUnitTest testPlayDebugUnitTest`.
- Run release-version tests with `python3 -m unittest discover -s scripts -p 'test_*.py'`.
- Release verification: `./gradlew lintGithubDebug lintGithubRelease lintPlayRelease assembleGithubRelease bundlePlayRelease`.
- Unsigned local release artifacts do not validate production signing. CI restores the upload key and verifies signed APK/AAB artifacts before publishing.
- A push to `master` triggers Android Beta, followed by Android Release on successful beta completion. Version generation is in `scripts/release_version.py`; preserve increasing build codes and matching beta/stable signing identities.
