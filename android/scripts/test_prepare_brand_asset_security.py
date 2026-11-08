"""Regression checks: user CLI arguments cannot redirect approved KX logo paths."""
import sys
import unittest
from pathlib import Path
from unittest.mock import patch

import prepare_brand_asset as brand


class BrandAssetPathSecurityTest(unittest.TestCase):
    def run_with(self, *args):
        with patch.object(sys, "argv", ["prepare_brand_asset.py", *args]), \
             patch.object(brand, "derive") as derive:
            try:
                brand.main()
            except SystemExit as exc:
                return exc.code, derive
            return 0, derive

    def test_defaults_use_only_repo_owned_paths(self):
        code, derive = self.run_with()
        self.assertEqual(code, 0)
        root = Path(brand.__file__).resolve().parents[2]
        derive.assert_called_once_with(
            root / "docs/brand/KX_MASTER_LOGO_ORIGINAL_RGBA.png",
            root / "android/app/src/main/res/drawable-nodpi/kx_brand.png",
        )

    def test_output_cannot_escape_repository(self):
        code, derive = self.run_with("--output", str(Path.home() / "malicious.png"))
        self.assertEqual(code, 2)
        derive.assert_not_called()

    def test_master_cannot_read_unauthorized_path(self):
        code, derive = self.run_with("--master", str(Path.home() / "secret.txt"))
        self.assertEqual(code, 2)
        derive.assert_not_called()

    def test_approved_absolute_paths_keep_existing_cli_working(self):
        root = Path(brand.__file__).resolve().parents[2]
        code, derive = self.run_with(
            "--master", str(root / "docs/brand/KX_MASTER_LOGO_ORIGINAL_RGBA.png"),
            "--output", str(root / "android/app/src/main/res/drawable-nodpi/kx_brand.png"),
        )
        self.assertEqual(code, 0)
        self.assertEqual(derive.call_count, 1)


if __name__ == "__main__":
    unittest.main()
