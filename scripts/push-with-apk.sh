#!/usr/bin/env bash
# Push source and optionally stage a compatible existing APK until CI replaces it.
set -euo pipefail
cd "$(git rev-parse --show-toplevel)"
[[ "$(git branch --show-current)" == master ]] || { echo 'Run from master.' >&2; exit 1; }
[[ -z "$(git status --porcelain --untracked-files=no)" ]] || { echo 'Commit tracked changes first.' >&2; exit 1; }
revision="$(git rev-parse HEAD)"
commit_time="$(git show -s --format=%ct HEAD)"
source_code=$((commit_time - 1577836800))
version="1.0.$source_code-beta"
tag="v$version"
apk="${1:-app/build/outputs/apk/github/debug/app-github-debug.apk}"
repo="mishalshanavas/Doomly"
git push origin master
[[ -f "$apk" ]] || { echo 'No local APK. GitHub will build and publish it.'; exit 0; }
# Never publish an APK older than the pushed source revision.
[[ "$(stat -c %Y "$apk")" -ge "$commit_time" ]] || { echo 'Local APK predates this commit; waiting for CI.'; exit 0; }
sdk="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
if [[ -z "$sdk" && -f local.properties ]]; then sdk="$(sed -n 's/^sdk.dir=//p' local.properties)"; fi
tools_dir="$(find "$sdk/build-tools" -mindepth 1 -maxdepth 1 -type d | sort -V | tail -1)"
work_dir="$(mktemp -d)"
trap 'rm -rf "$work_dir"' EXIT
# A temporary APK must be installable over the published app and replaceable by CI.
gh release download --repo "$repo" --pattern '*.apk' --dir "$work_dir"
reference="$(find "$work_dir" -name '*.apk' -print -quit)"
cert() { "$tools_dir/apksigner" verify --print-certs "$1" | sed -n '/certificate SHA-256 digest:/p' | sort; }
local_cert="$(cert "$apk")"
[[ -n "$local_cert" && "$local_cert" == "$(cert "$reference")" ]] || { echo 'Local APK uses a different signing key; waiting for signed CI build.'; exit 0; }
"$tools_dir/aapt" dump badging "$apk" > "$work_dir/badging"
python3 - "$work_dir/badging" "$work_dir/details" "$source_code" <<'PY'
import re,sys
text=open(sys.argv[1]).readline()
m=re.search(r"package: name='([^']+)' versionCode='(\d+)' versionName='([^']+)'", text)
if not m or m[1] != 'com.doomly.app' or int(m[2]) > int(sys.argv[3]):
    raise SystemExit('Local APK is incompatible with this CI replacement.')
open(sys.argv[2], 'w').write(f'Build code: {m[2]}\nAPK version: {m[3]}\nTemporary local APK. GitHub is building the replacement.\n')
PY
cp "$apk" "$work_dir/Doomly-$version.apk"
# Do not overwrite an already completed CI release.
if gh release view "$tag" --repo "$repo" --json body --jq .body > "$work_dir/body" 2>/dev/null; then
    echo 'Release already exists; leaving its APK intact.'
    exit 0
fi
gh release create "$tag" "$work_dir/Doomly-$version.apk" --repo "$repo" --target "$revision" --prerelease --title "Doomly $version · building" --notes-file "$work_dir/details"
echo 'Temporary APK published. CI will replace the same release asset when ready.'
