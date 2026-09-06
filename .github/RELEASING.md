# Doomly Android releases

The `Android Release` workflow builds a signed, minified APK and Android App
Bundle. Configure these repository Actions secrets before running it:

- `ANDROID_KEYSTORE_BASE64`: the upload keystore encoded as one Base64 string
- `ANDROID_KEYSTORE_PASSWORD`: upload keystore password
- `ANDROID_KEY_ALIAS`: upload key alias
- `ANDROID_KEY_PASSWORD`: upload key password
- `GOOGLE_SERVICES_JSON`: optional replacement for `app/google-services.json`

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
1,000 + patch`. A manual workflow run asks for an explicit version name and
increasing version code and uploads the files as workflow artifacts without
creating a GitHub Release.
