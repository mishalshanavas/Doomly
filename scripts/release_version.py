import argparse
import json
import os
import re
import subprocess
from pathlib import Path

EPOCH = 1577836800
MAX_CODE = 2100000000
STABLE_VERSION = re.compile(r"(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)(?:\+[0-9A-Za-z-]+(?:\.[0-9A-Za-z-]+)*)?")


def resolve_version(channel, commit_seconds, ref="", event="", version_name="", version_code="", latest=None):
    source = commit_seconds - EPOCH
    if source <= 0:
        raise ValueError("Commit date must be after the version epoch")
    if channel == "beta":
        name, code = f"1.0.{source}-beta", source + 1
    elif channel == "stable":
        name = ref.removeprefix("refs/tags/v") if ref.startswith("refs/tags/v") else f"1.0.{source}"
        code = source + 2
        if event == "workflow_dispatch":
            name = version_name
            if not re.fullmatch(r"[1-9][0-9]*", version_code):
                raise ValueError("Version code must be a positive integer")
            code = int(version_code)
        if not STABLE_VERSION.fullmatch(name):
            raise ValueError("Stable releases require a stable semantic version, for example 1.2.3")
    else:
        raise ValueError("Unknown release channel")
    if not 1 <= code <= MAX_CODE:
        raise ValueError("Version code must be from 1 through 2100000000")
    tag = f"v{name}"
    if latest:
        match = re.search(r"(?m)^Build code: ([0-9]+)\s*$", latest.get("body") or "")
        if not match:
            raise ValueError("Latest release has no build code; verify its version before publishing")
        previous = int(match[1])
        if code < previous or (code == previous and latest.get("tag_name") != tag):
            raise ValueError("Release would not upgrade the latest published APK")
    result = {
        "DOOMLY_VERSION_NAME": name,
        "DOOMLY_VERSION_CODE": str(code),
        "RELEASE_BASENAME": f"Doomly-{name}",
        "RELEASE_TAG": tag,
    }
    if channel == "stable" and event == "workflow_run":
        result["BETA_RELEASE_TAG"] = f"v1.0.{source}-beta"
    return result


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("channel", choices=("beta", "stable"))
    parser.add_argument("--latest-release", type=Path)
    args = parser.parse_args()
    latest = json.loads(args.latest_release.read_text()) if args.latest_release else None
    seconds = int(subprocess.check_output(["git", "show", "-s", "--format=%ct", "HEAD"], text=True))
    values = resolve_version(
        args.channel, seconds, os.getenv("GITHUB_REF", ""), os.getenv("GITHUB_EVENT_NAME", ""),
        os.getenv("INPUT_VERSION_NAME", ""), os.getenv("INPUT_VERSION_CODE", ""), latest,
    )
    values["RELEASE_COMMIT"] = subprocess.check_output(["git", "rev-parse", "HEAD"], text=True).strip()
    for name, value in values.items():
        print(f"{name}={value}")


if __name__ == "__main__":
    main()
