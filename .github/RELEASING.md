# Doomly Android releases

The `Android Release` workflow builds a signed, minified GitHub APK with the
self-updater and a Play-safe Android App Bundle without sideload permissions.
Configure these repository Actions secrets before running it:

- `ANDROID_KEYSTORE_BASE64`: the upload keystore encoded as one Base64 string
- `ANDROID_KEYSTORE_PASSWORD`: upload keystore password
- `ANDROID_KEY_ALIAS`: upload key alias
- `ANDROID_KEY_PASSWORD`: upload key password

The Doomly upload key generated for this project is stored locally at
`C:\Users\mishal\.android\doomly-upload.jks`. Its credential backup is encrypted
to the current Windows account with DPAPI at
`C:\Users\mishal\.android\doomly-upload-credential.clixml`. Back up both files
securely; losing the upload key can prevent future updates.

To recover the alias and password on this Windows account:

```powershell
$credential = Import-Clixml "C:\Users\mishal\.android\doomly-upload-credential.clixml"
$credential.UserName
$credential.GetNetworkCredential().Password
```

On PowerShell, encode an existing upload keystore without modifying it:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("C:\path\doomly-upload.jks")) | Set-Clipboard
```

To publish GitHub Release `v1.2.3` with both files attached:

```powershell
git tag v1.2.3
git push origin v1.2.3
```

Tag builds generate the Play `versionCode` as `major * 1,000,000 + minor *
1,000 + patch`. Manual builds use the supplied version code.

Each push to `master` first publishes a signed debug APK as a beta prerelease.
After `Android Beta` succeeds, `Android Release` checks out the same commit,
tests and builds the signed, optimized APK and Play AAB, and publishes a stable
GitHub Release. Only after publication succeeds is that commit's temporary beta
release removed. If the release build fails, the beta stays available.

Automatic versions use `1.0.<commit timestamp minus 2020-01-01 UTC>` (with
`-beta` for the debug build). The beta version code is that timestamp offset
plus one; the stable build uses plus two so Android can upgrade from the beta.
Both builds use the same upload signing key and application ID. Signing secrets
are required for both workflows. Keep manually supplied version codes increasing
relative to automatic builds too.
