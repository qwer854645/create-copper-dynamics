"""Re-map a Create-derived texture onto a target ramp by rank, not by absolute thresholds.

`tools/recolor-smart-fluid-tank.cjs` and `tools/repaint-smart-tank.py` classified every warm
pixel with a fixed absolute luminance ladder:

    thresholds = [35, 55, 75, 90, 108, 128, 150, 180]

Create's eight copper tones sit at luminance 86.4 / 94.0 / 104.5 / 113.1 / 123.3 / 131.7 /
140.0 / 149.0, which that ladder collapses to indices [3, 4, 4, 5, 5, 6, 6, 6] - four
distinct outputs out of eight, and every one of them lifted 70..100 luminance. That is why
the smart tank lost its shading and reads as flat gold paint.

This script keeps the structure bit-for-bit and only re-maps the tones: source tone k (by
luminance rank) -> target ramp tone k. The non-warm guard (grey mechanical parts) is kept.

Usage:
    python tools/recolor-rank.py <create-source> [--check]
    <create-source> is either the Create jar or an extracted assets root.
"""
from __future__ import annotations

import io
import importlib.util
import math
import sys
import zipfile
from collections import Counter
from pathlib import Path

from PIL import Image

# tools/ names use hyphens by project convention, so load the shared module by path.
_spec = importlib.util.spec_from_file_location(
    "copper_material", Path(__file__).resolve().parent / "copper-material.py")
_cm = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(_cm)

BRASS = _cm.BRASS
metrics = _cm.metrics

ROOT = Path(__file__).resolve().parents[1]
TEXTURES = ROOT / "src/main/resources/assets/ccd/textures/block/smart_fluid_tank"

# 8 step brass ladder shared with tools/copper-material.py
TARGET = BRASS


def is_warm(c):
    r, g, b = c[0], c[1], c[2]
    return r >= g and g >= b and (r - b) >= 20 and (g - b) >= 8


def luminance(c):
    return 0.2126 * c[0] + 0.7152 * c[1] + 0.0722 * c[2]


def load_create(src: Path, name: str) -> Image.Image | None:
    rel = f"assets/create/textures/block/{name}"
    if src.is_file():
        with zipfile.ZipFile(src) as zf:
            try:
                return Image.open(io.BytesIO(zf.read(rel))).convert("RGBA")
            except KeyError:
                return None
    p = src / rel
    if not p.exists():
        p = src / "block" / name
    return Image.open(p).convert("RGBA") if p.exists() else None


def rank_map(img: Image.Image, target):
    """Rebuild `img` with every warm tone replaced by its rank counterpart in `target`."""
    warm = sorted({c[:3] for c in img.getdata() if c[3] > 0 and is_warm(c)}, key=luminance)
    if not warm:
        return img.copy(), {}
    steps = len(target)
    table = {}
    for k, c in enumerate(warm):
        if len(warm) == 1:
            idx = steps // 2
        else:
            idx = round(k * (steps - 1) / (len(warm) - 1))
        table[c] = tuple(int(target[idx][i : i + 2], 16) for i in (0, 2, 4))
    out = img.copy()
    px = out.load()
    for y in range(out.height):
        for x in range(out.width):
            r, g, b, a = px[x, y]
            if a and is_warm((r, g, b)):
                nr, ng, nb = table[(r, g, b)]
                px[x, y] = (nr, ng, nb, a)
    return out, table


def corr(a: Image.Image, b: Image.Image) -> float:
    la = [luminance(c) for c in a.getdata()]
    lb = [luminance(c) for c in b.getdata()]
    ma = sum(la) / len(la)
    mb = sum(lb) / len(lb)
    da = math.sqrt(sum((v - ma) ** 2 for v in la) / len(la))
    db = math.sqrt(sum((v - mb) ** 2 for v in lb) / len(lb))
    if da == 0 or db == 0:
        return 0.0
    return sum((x - ma) * (y - mb) for x, y in zip(la, lb)) / (len(la) * da * db)


TARGETS = [
    "fluid_tank.png",
    "fluid_tank_top.png",
    "fluid_tank_connected.png",
    "fluid_tank_inner.png",
]


def main(argv):
    if len(argv) < 2:
        print(__doc__)
        return 2
    src = Path(argv[1]).resolve()
    check = "--check" in argv

    print(f"{'texture':32s} {'source':26s} {'corr':>6s}  std          flat        tones")
    for name in TARGETS:
        path = TEXTURES / name
        current = Image.open(path).convert("RGBA")
        best = None
        for cand_name in [name, "copper_underside.png", "fluid_tank_inner.png",
                          "fluid_tank_connected.png", "fluid_tank_top_connected.png"]:
            cand = load_create(src, cand_name)
            if cand is None or cand.size != current.size:
                continue
            if cand.getchannel("A").tobytes() != current.getchannel("A").tobytes():
                continue
            rebuilt, _ = rank_map(cand, TARGET)
            score = corr(rebuilt, current)
            if best is None or score > best[0]:
                best = (score, cand_name, rebuilt)
        if best is None:
            print(f"{name:32s} no Create source with matching size/alpha - skipped")
            continue
        score, src_name, rebuilt = best
        before, after = metrics(current), metrics(rebuilt)
        print(f"{name:32s} {src_name:26s} {score:6.3f}  "
              f"{before['std']:5.1f}->{after['std']:5.1f} {before['flat']:5.1f}->{after['flat']:5.1f}% "
              f"{before['tones']:2d}->{after['tones']:2d}")
        if not check:
            rebuilt.save(path)
    print("\ncheck only, nothing written" if check else "\nwritten")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
