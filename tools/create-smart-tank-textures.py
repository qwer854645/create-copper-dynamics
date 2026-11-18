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

SIDE_SEAM = {3, 12}

# `fluid_tank.png` is a FRAME ATLAS, not one 16x16 face. From the model's UVs:
#
#   rows 0..3   (uv [0,0,16,4])    the lid's side band      -> horizontal edge, top
#   rows 12..15 (uv [0,12,16,16])  the bottom band          -> horizontal edge, bottom
#   cols 0..3 / 12..15, rows 4..12 (uv [0,4,4,12] / [12,4,16,12]) -> the vertical corner strips
#   the middle is behind the glass window and never sampled
#
# Ringing the whole 16x16 face (an earlier attempt) decorated pixels the model never reads and
# left the actual bands wrong.
#
# The two axes still get different values, because the horizontal edge is shared with the block
# above/below: bright brass there turns a tall tank into a stack of stripes.
V_STRIP = {0, 1, 2, 3, 12, 13, 14, 15}     # columns of the vertical strips
H_BAND_TOP = {0, 1}                          # dim brass rim rows (top end)
H_BAND_BOT = {14, 15}                        # dim brass rim rows (bottom end)

# Bands and posts map the source tone into a NARROW gold range instead of a single pinned colour.
# A pinned colour filled the whole band/plate with one flat value, which read as a big dead colour
# block ("这样大色块有些不太好看"). A two-step range keeps the band reading as one brass band while
# letting Create's own shading show through, so the surface still has texture.
POST_HI_LO, POST_HI_HI = 7, 9      # outer post pixel
POST_LO_LO, POST_LO_HI = 5, 7      # second post pixel
EDGE_LO, EDGE_HI = 2, 5            # horizontal rim


def _lum(c):
    return 0.2126 * c[0] + 0.7152 * c[1] + 0.0722 * c[2]


def zone_side(x, y):
    lx, ly = x % 16, y % 16
    top_or_bot = ly in H_BAND_TOP or ly in H_BAND_BOT
    if lx in V_STRIP and not top_or_bot:
        # vertical corner post: bright outside, one step in, a dark bevel, then copper again
        if lx in (0, 15):
            return ("gold_range", POST_HI_LO, POST_HI_HI)
        if lx in (1, 14):
            return ("gold_range", POST_LO_LO, POST_LO_HI)
        if lx in (2, 13):
            return "seam"
        return "copper"
    if top_or_bot:
        return ("gold_range", EDGE_LO, EDGE_HI)
    if ly in SIDE_SEAM:
        return "seam"
    return "copper"


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


# ---------------------------------------------------------------------------
# The 64 px `*_connected` CT sheets are NOT a 4x4 grid of 16 px tiles. Create lays each one out
# as a single 48x48 panel (a 3x3 nine-slice of the connected structure) at x16..63 / y0..47,
# plus the plain 16x16 face at x0..15 / y48..63, with the rest transparent. Treating them as a
# tile grid put a brass ring around every 16 px cell and turned one continuous panel into a
# visible 3x3 grid - which is exactly what the multi-block tower showed.
CONN_REGION = (16, 0, 48, 48)
CONN_PLAIN = (0, 48, 16, 16)

# Rib rows of the connected side panel, read off Create's art: bright rib tops and dark seams.
SIDE48_BAND = {0, 1, 2, 4, 9, 25, 41, 45}
SIDE48_SEAM = {3, 8, 24, 40, 44, 47}


def zone_connected_side(x, y):
    rx, ry, rw, rh = CONN_REGION
    if rx <= x < rx + rw and ry <= y < ry + rh:
        sx, sy = x - rx, y - ry
        # the 48 px panel is the whole tank's face: same grammar, scaled to its own bands
        if sx in (0, rw - 1):
            return ("gold_range", POST_HI_LO, POST_HI_HI)
        if sx in (1, rw - 2):
            return ("gold_range", POST_LO_LO, POST_LO_HI)
        if sy in (0, 1, rh - 2, rh - 1):
            return ("gold_range", EDGE_LO, EDGE_HI)
        if sx in (2, rw - 3):
            return "seam"
        if sy in SIDE48_SEAM:
            return "seam"
        return "copper"
    if y >= CONN_PLAIN[1]:
        return zone_side(x, y)          # the plain 16x16 face keeps the standalone grammar
    return "copper"


def zone_connected_ring(width, seam):
    def f(x, y):
        rx, ry, rw, rh = CONN_REGION
        if rx <= x < rx + rw and ry <= y < ry + rh:
            lx, ly, w, h = x - rx, y - ry, rw, rh
        elif y >= CONN_PLAIN[1]:
            lx, ly, w, h = x, y - CONN_PLAIN[1], 16, 16
        else:
            return "copper"
        d = min(lx, ly, w - 1 - lx, h - 1 - ly)
        if d < width:
            return "brass"
        if seam and d < width + seam:
            return "seam"
        return "copper"
    return f


# The lid gets a full brass casing as well: 2 px of frame plus a 1 px shadow so it reads like the
# side bands, instead of the single-pixel line it had before.
ZONES = {
    "fluid_tank": (zone_side, None),
    "fluid_tank_connected": (zone_connected_side, None),
    "fluid_tank_top": (lambda x, y, w=16, h=16: zone_ring(x, y, w, h, 2, seam=1), None),
    "fluid_tank_top_connected": (zone_connected_ring(2, 1), None),
    "fluid_tank_inner": (lambda x, y, w=16, h=16: zone_ring(x, y, w, h, 1, seam_row=15), None),
    "fluid_tank_inner_connected": (zone_connected_ring(1, 0), None),
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
            if isinstance(zone, tuple) and zone[0] == "gold_range":
                # Map the source tone into a narrow gold range instead of pinning one colour:
                # a single colour per band filled it with one flat value, which read as a big dead
                # colour block. The range keeps Create's own shading inside the band.
                lo, hi = zone[1], zone[2]
                t = rank[(r, g, b)]
                op[x, y] = GOLD[min(len(GOLD) - 1, lo + round(t * (hi - lo)))] + (a,)
                continue
            if isinstance(zone, tuple):
                op[x, y] = GOLD[zone[1]] + (a,)
                continue
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
