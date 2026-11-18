"""Textures for the one-block-tall collection grates.

The model (`models/block/copper_collection_grate.json`) samples very specific bands out of a
16x16 sheet, so the artwork has to be laid out to match instead of being a generic plate:

  v(row)  who samples it
  0..1    raised lip underside            -> must read as shadow
  1..2    upper bowl overhang underside   -> must read as shadow
  2..3    lower bowl top / lip sides      -> thin edge
  3..9    UPPER bowl outer wall, 6 px     -> v3 is the top edge, v9 the bottom
  5..11   LOWER bowl outer wall, 6 px     -> v5 is the top edge, v11 the bottom
  7..8    floor side, 1 px                -> bottom-most visible course
  0,1/14,15 + 0,1/14,15 columns on `rim`   -> the lip's top ring

That is why `create:block/copper_underside` cannot be reused for the hull: its bright
highlight sits on row 1, exactly where the overhang underside needs a shadow, and it has no
vertical falloff, so the two tiers read as one flat slab.

All colours come from Create's own eight copper tones (see tools/copper-material.py).

Usage:
    python tools/create-collection-grate-textures.py <ccd-assets-root> [--check]
"""
from __future__ import annotations

import importlib.util
import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]

_spec = importlib.util.spec_from_file_location(
    "copper_material", Path(__file__).resolve().parent / "copper-material.py")
_cm = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(_cm)

COPPER = _cm.PAL_COPPER
BRASS = _cm.PAL_BRASS
N = 16

# Row -> (tone index, horizontal jitter). Tone 0 is the darkest copper, 7 the brightest.
#
# Rows 3..9 are the upper tier and rows 5..11 the lower tier, so 5..9 is shared. Row 5 lands
# on "top of the lower wall" = directly under the overhang, and on "2 px below the top" for
# the upper wall; both want to be dark, so row 5 is the one deliberate groove. Row 9 is the
# upper tier's bottom, which sits on the lower tier, so it darkens as well.
WALL_ROWS = [
    (1, 0),   # 0  lip underside
    (1, 0),   # 1  overhang underside
    (2, 0),   # 2  overhang underside, inner
    (5, 0),   # 3  upper wall top edge   <- upper bowl starts here
    (4, 0),   # 4
    (2, 0),   # 5  groove: lower wall top under the overhang
    (4, 0),   # 6
    (4, 0),   # 7  floor side course
    (4, 0),   # 8
    (3, 0),   # 9  upper wall bottom   <- upper bowl ends here
    (3, 0),   # 10
    (2, 0),   # 11 lower wall bottom   <- lower bowl ends here
    (1, 0),   # 12
    (1, 0),   # 13
    (1, 0),   # 14
    (0, 0),   # 15
]

# Column jitter so the wall does not read as a printed gradient or a corrugated ruler. Kept
# to +/-1 tone, so the four sides still meet plausibly at the corners.
WALL_COLS = {2: 1, 3: 0, 6: -1, 7: 0, 10: 1, 11: 0, 13: -1}
# Rows that get a little vertical wobble, so the panel lines are not perfectly straight.
WALL_WOBBLE = {3: (5, 8), 5: (3, 6), 9: (10, 13)}


def _clamp(v, lo, hi):
    return max(lo, min(hi, v))


def wall(palette=COPPER):
    im = Image.new("RGBA", (N, N))
    for y, (tone, jit) in enumerate(WALL_ROWS):
        for x in range(N):
            t = tone + jit + WALL_COLS.get(x, 0)
            # a 1 tone dip over part of a row breaks the ruler look without adding noise
            lo, hi = WALL_WOBBLE.get(y, (N, N))
            if lo <= x <= hi:
                t -= 1
            im.putpixel((x, y), palette[_clamp(t, 0, len(palette) - 1)] + (255,))
    # two small bright wear marks on the top edge
    for (x, y) in ((4, 3), (5, 3), (12, 3), (2, 5), (13, 5)):
        im.putpixel((x, y), palette[6] + (255,))
    return im


def rim(palette=COPPER):
    """The raised lip's top face. Only the outer two rows/columns are ever sampled.

    Row/column 0 and 15 are the lip's outer edge on the matching side, so the ring has to be
    mirror symmetric: bright chamfer outside, dark shadow line where the lip drops into the
    bowl.
    """
    im = Image.new("RGBA", (N, N))
    for y in range(N):
        for x in range(N):
            ring = min(x, y, N - 1 - x, N - 1 - y)
            if ring == 0:
                t = 6
            elif ring == 1:
                t = 1
            else:
                t = 4
            im.putpixel((x, y), palette[_clamp(t, 0, len(palette) - 1)] + (255,))
    # corner caps: a lit bevel instead of a continuous band
    for (cx, cy) in ((0, 0), (N - 1, 0), (0, N - 1), (N - 1, N - 1)):
        im.putpixel((cx, cy), palette[7] + (255,))
    for (cx, cy) in ((1, 0), (0, 1), (N - 2, 0), (N - 1, 1),
                     (1, N - 1), (0, N - 2), (N - 2, N - 1), (N - 1, N - 2)):
        im.putpixel((cx, cy), palette[3] + (255,))
    return im


def lip(palette=BRASS):
    """Brass lip side band. The smart model's `brass` key samples rows 2..3 only."""
    im = Image.new("RGBA", (N, N))
    rows = [1, 1, 6, 3, 2, 2, 1, 1, 2, 2, 1, 1, 1, 1, 1, 1]
    for y, tone in enumerate(rows):
        for x in range(N):
            im.putpixel((x, y), palette[_clamp(tone, 0, len(palette) - 1)] + (255,))
    return im


TARGETS = {
    "block/copper_collection_grate/wall.png": lambda: wall(COPPER),
    "block/copper_collection_grate/rim.png": lambda: rim(COPPER),
    "block/smart_collection_grate/body.png": lambda: lip(BRASS),
    "block/smart_collection_grate/rim.png": lambda: rim(BRASS),
}


def main(argv):
    if len(argv) < 2:
        print(__doc__)
        return 2
    assets = Path(argv[1]).resolve()
    if assets.name == "assets":
        assets = assets / "ccd"
    check = "--check" in argv

    print(f"{'texture':44s} {'std':>5s} {'flat':>6s} {'tones':>5s}")
    for rel, fn in sorted(TARGETS.items()):
        im = fn()
        m = _cm.metrics(im)
        print(f"{rel:44s} {m['std']:5.1f} {m['flat']:5.1f}% {m['tones']:5d}")
        if not check:
            path = assets / "textures" / rel
            path.parent.mkdir(parents=True, exist_ok=True)
            im.save(path)
    print("\ncheck only" if check else f"\n{len(TARGETS)} textures written")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
