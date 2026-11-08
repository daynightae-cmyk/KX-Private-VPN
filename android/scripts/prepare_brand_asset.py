#!/usr/bin/env python3
"""Derive the runtime KX orb from the owner-approved master artwork.

The master PNG is never edited in place. It is preserved byte-for-byte at
docs/brand/KX_MASTER_LOGO_ORIGINAL_RGBA.png (SHA-256
f336992dedba8f80580fc550735b0629c595f85d8c2a0587f08bf6f6fb2775e8).

The only derivation is a narrow alpha feather at the circular outer edge. The
master has a baked dark outline at radius ~532-539 px (1078 px canvas) that
renders as a black rim on light backgrounds. Everything inside radius 526 px
is copied without changes; 526-531.5 px fades to transparent so no dark edge
survives scaling. No crop, colour replacement, redraw or AI regeneration.
"""
from __future__ import annotations

import argparse
import hashlib
from pathlib import Path

from PIL import Image


CENTER = 538.5
FEATHER_START = 526.0
FEATHER_END = 531.5
EXPECTED_MASTER_SHA256 = (
    "f336992dedba8f80580fc550735b0629c595f85d8c2a0587f08bf6f6fb2775e8"
)


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def smoothstep(edge0: float, edge1: float, value: float) -> float:
    if value <= edge0:
        return 0.0
    if value >= edge1:
        return 1.0
    t = (value - edge0) / (edge1 - edge0)
    return t * t * (3.0 - 2.0 * t)


def derive(master: Path, output: Path) -> None:
    source_digest = sha256(master)
    if source_digest != EXPECTED_MASTER_SHA256:
        raise SystemExit(
            f"refusing to derive from an unexpected master: {source_digest}"
        )

    image = Image.open(master).convert("RGBA")
    width, height = image.size
    if (width, height) != (1078, 1078):
        raise SystemExit(f"unexpected master dimensions: {width}x{height}")

    pixels = list(image.getdata())
    cleaned: list[tuple[int, int, int, int]] = []
    for index, (red, green, blue, alpha) in enumerate(pixels):
        y, x = divmod(index, width)
        distance = ((x - CENTER) ** 2 + (y - CENTER) ** 2) ** 0.5
        # 1.0 inside the artwork, 0.0 beyond the baked dark edge.
        coverage = 1.0 - smoothstep(FEATHER_START, FEATHER_END, distance)
        cleaned.append((red, green, blue, round(alpha * coverage)))

    result = Image.new("RGBA", image.size)
    result.putdata(cleaned)
    output.parent.mkdir(parents=True, exist_ok=True)
    result.save(output, format="PNG", optimize=True)
    print(f"master sha256: {source_digest}")
    print(f"derived: {output}")
    print(f"derived sha256: {sha256(output)}")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--master",
        type=Path,
        default=Path("docs/brand/KX_MASTER_LOGO_ORIGINAL_RGBA.png"),
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=Path(
            "android/app/src/main/res/drawable-nodpi/kx_brand.png"
        ),
    )
    args = parser.parse_args()
    derive(args.master, args.output)


if __name__ == "__main__":
    main()
