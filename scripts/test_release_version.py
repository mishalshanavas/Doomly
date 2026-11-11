import unittest
from release_version import EPOCH, resolve_version


class ReleaseVersionTest(unittest.TestCase):
    seconds = EPOCH + 216121200

    def test_stable_upgrades_same_commit_beta(self):
        beta = resolve_version("beta", self.seconds)
        stable = resolve_version("stable", self.seconds, event="workflow_run")
        self.assertEqual("1.0.216121200-beta", beta["DOOMLY_VERSION_NAME"])
        self.assertEqual("1.0.216121200", stable["DOOMLY_VERSION_NAME"])
        self.assertGreater(int(stable["DOOMLY_VERSION_CODE"]), int(beta["DOOMLY_VERSION_CODE"]))
        self.assertEqual(beta["RELEASE_TAG"], stable["BETA_RELEASE_TAG"])

    def test_tag_uses_same_code_scheme_as_automatic_release(self):
        tagged = resolve_version("stable", self.seconds, "refs/tags/v1.2.3", "push")
        self.assertEqual("1.2.3", tagged["DOOMLY_VERSION_NAME"])
        self.assertEqual("216121202", tagged["DOOMLY_VERSION_CODE"])

    def test_dispatch_uses_explicit_version(self):
        value = resolve_version("stable", self.seconds, event="workflow_dispatch",
                                version_name="2.0.0", version_code="216121300")
        self.assertEqual("216121300", value["DOOMLY_VERSION_CODE"])

    def test_invalid_dispatch_versions_are_rejected(self):
        for name, code in [("1.0.0-beta", "200"), ("1.0.0\nEVIL=value", "200"),
                           ("01.0.0", "200"), ("1.0.0", "0"), ("1.0.0", "2100000001")]:
            with self.subTest(name=name, code=code), self.assertRaises(ValueError):
                resolve_version("stable", self.seconds, event="workflow_dispatch",
                                version_name=name, version_code=code)

    def test_old_or_equal_code_cannot_replace_different_release(self):
        for code in (216121202, 216121203):
            with self.subTest(code=code), self.assertRaises(ValueError):
                resolve_version("stable", self.seconds, latest={"tag_name": "v2.0.0", "body": f"Build code: {code}"})

    def test_same_release_can_be_retried(self):
        resolve_version("stable", self.seconds, latest={
            "tag_name": "v1.0.216121200", "body": "Build code: 216121202\n\nNotes"})

    def test_missing_published_code_fails_closed(self):
        with self.assertRaises(ValueError):
            resolve_version("stable", self.seconds, latest={"tag_name": "v1.0.0", "body": ""})

    def test_beta_cannot_replace_already_published_stable(self):
        with self.assertRaises(ValueError):
            resolve_version("beta", self.seconds, latest={
                "tag_name": "v1.0.216121200", "body": "Build code: 216121202"})


if __name__ == "__main__":
    unittest.main()
