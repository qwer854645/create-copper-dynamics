"""智能储罐 / smart fluid tank: a copper tank with a brass casing frame.

The tank keeps Create's copper body exactly as drawn; only the frame is turned into brass, and
the panel seams become neutral dark metal.

Why not one global recolour (the two earlier attempts):

  * a straight copper->brass rank map stretched the tone span from 63 to 95 and turned Create's
    quiet tank ribs into noise: `std` 30.5 against Create's 18.7;
  * making the whole tank brass and using `brass_casing`'s darks (#442B15 / #53381A - low
    saturation warm browns) read as dark wood at 16 px.

`create:block/fluid_tank.png` already has the right structure for a casing, so zones decide the
material instead of tone alone:

    row 0..2, 4, 13..14   horizontal bands   -> BRASS (the casing frame)
    row 3, 12, 15         panel seams        -> NEUTRAL DARK metal (no warm brown)
    row 5..11             body panels        -> COPPER, untouched

The lid and the inner wall get an outer ring instead of horizontal bands.

Usage:
    python tools/create-smart-tank-textures.py <ccd-assets-root> <create-source> [--check|--verify]
"""
from __future__ import annotations

import importlib.util
import io
import sys
import zipfile
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]

_spec = importlib.util.spec_from_file_location(
    "copper_material", Path(__file__).resolve().parent / "copper-material.py")
_cm = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(_cm)

# create:block/brass_block.png, the gold stretch
GOLD = [(0x9E, 0x69, 0x47), (0xAF, 0x79, 0x4F), (0xBF, 0x8E, 0x55), (0xC7, 0x97, 0x58),
        (0xCE, 0xA0, 0x5A), (0xD7, 0xAA, 0x5E), (0xE4, 0xB7, 0x63), (0xF7, 0xCB, 0x6C),
        (0xFB, 0xDC, 0x7D), (0xFF, 0xEB, 0x8C)]
# create:block/industrial_iron_block.png, neutral greys - never a warm brown, so no wood read
IRON = [(0x35, 0x34, 0x38), (0x3F, 0x3E, 0x42), (0x49, 0x48, 0x48), (0x4F, 0x4F, 0x4F)]

SIDE_BAND = {0, 1, 2, 4, 13, 14}
SIDE_SEAM = {3, 12, 15}


def _lum(c):
    return 0.2126 * c[0] + 0.7152 * c[1] + 0.0722 * c[2]


def zone_side(x, y):
    r = y % 16
    return "brass" if r in SIDE_BAND else ("seam" if r in SIDE_SEAM else "copper")


def zone_ring(x, y, w, h, width, seam=0, seam_row=None):
    """Outer `width` px = brass casing, the next `seam` px = neutral shadow, rest copper."""
    if seam_row is not None and y == seam_row:
        return "seam"
    d = min(x, y, w - 1 - x, h - 1 - y)
    if d < width:
        return "brass"
    if seam and d < width + seam:
        return "seam"
    return "copper"


# The lid gets a full brass casing as well: 2 px of frame plus a 1 px shadow so it reads like the
# side bands, instead of the single-pixel line it had before. The 64 px CT sheets scale 4x.
ZONES = {
    "fluid_tank": (zone_side, None),
    "fluid_tank_connected": (zone_side, None),
    "fluid_tank_top": (lambda x, y, w=16, h=16: zone_ring(x, y, w, h, 2, seam=1), None),
    "fluid_tank_top_connected": (lambda x, y, w=64, h=64: zone_ring(x, y, w, h, 8, seam=4), None),
    "fluid_tank_inner": (lambda x, y, w=16, h=16: zone_ring(x, y, w, h, 1, seam_row=15), None),
    "fluid_tank_inner_connected": (lambda x, y, w=64, h=64: zone_ring(x, y, w, h, 4), None),
    # glass windows: Create's own, kept verbatim
    "fluid_tank_window": (None, "copy"),
    "fluid_tank_window_single": (None, "copy"),
}


def load_create(src: Path, rel: str) -> Image.Image:
    key = f"assets/create/textures/block/{rel}"
    if src.is_file():
        with zipfile.ZipFile(src) as zf:
            return Image.open(io.BytesIO(zf.read(key))).convert("RGBA")
    for c in (src / key, src / "block" / rel, src / rel):
        if c.exists():
            return Image.open(c).convert("RGBA")
    raise FileNotFoundError(rel)


def convert(im: Image.Image, zonefn) -> Image.Image:
    px = im.load()
    w, h = im.size
    tones = sorted({px[x, y][:3] for y in range(h) for x in range(w) if px[x, y][3]}, key=_lum)
    rank = {c: (k / (len(tones) - 1) if len(tones) > 1 else 0.5) for k, c in enumerate(tones)}
    out = im.copy()
    op = out.load()
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if not a:
                continue
            zone = zonefn(x, y)
            if zone == "copper":
                continue                     # Create's copper stays exactly as drawn
            t = rank[(r, g, b)]
            ramp = GOLD if zone == "brass" else IRON
            op[x, y] = ramp[min(len(ramp) - 1, round(t * (len(ramp) - 1)))] + (a,)
    return out


def derive(create_src: Path):
    out = {}
    for name, (zonefn, mode) in ZONES.items():
        src = load_create(create_src, name + ".png")
        out[f"block/smart_fluid_tank/{name}.png"] = src if mode == "copy" else convert(src, zonefn)
    return out


def main(argv):
    if len(argv) < 3:
        print(__doc__)
        return 2
    assets = Path(argv[1]).resolve()
    if assets.name == "assets":
        assets = assets / "ccd"
    positional = [a for a in argv[2:] if not a.startswith("--")]
    create_src = Path(positional[0]).resolve()
    check = "--check" in argv
    verify = "--verify" in argv

    built = derive(create_src)

    if verify:
        drift = 0
        for rel, want in sorted(built.items()):
            path = assets / "textures" / rel
            if not path.exists():
                print(f"MISSING  {rel}")
                drift += 1
                continue
            have = Image.open(path).convert("RGBA")
            if have.size != want.size or have.tobytes() != want.tobytes():
                print(f"DRIFT    {rel}")
                drift += 1
        if drift:
            print(f"\n{drift} texture(s) no longer match; run without --verify to regenerate")
            return 1
        print(f"all {len(built)} smart tank textures match")
        return 0

    print(f"{'texture':46s} {'size':7s} {'std':>5s} {'tones':>5s}")
    for rel, im in sorted(built.items()):
        m = _cm.metrics(im)
        print(f"{rel:46s} {im.width}x{im.height:<4d} {m['std']:5.1f} {m['tones']:5d}")
        if not check:
            path = assets / "textures" / rel
            path.parent.mkdir(parents=True, exist_ok=True)
            im.save(path)
    print("\ncheck only" if check else f"\n{len(built)} textures written")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
