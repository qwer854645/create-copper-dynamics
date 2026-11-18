"""Textures for the fluid hatches, which now just inherit Create's item hatch model.

`models/block/fluid_hatch_closed.json` and `smart_fluid_hatch_closed.json` are only

    {"parent": "create:block/item_hatch/block_closed"}

so both blocks currently render pixel-identical to Create's item hatch. The parent exposes
exactly three overridable keys, and this tool fills them in:

    0  the housing      Create `hatch.png`, a 32x32 hand-packed atlas (2x supersampled,
                        every face picks its own small UV window)
    1  the flap face    Create `hatch_closed.png`, a 16x16 sheet; the flap samples
                        uv [2,3]-[14,14], i.e. a 12 wide x 11 tall plate
    particle            set to the housing

The housing atlas is **recoloured, never redrawn**: its UV windows are hand-packed, so any
new artwork would have to be laid out blind. Only luminance is remapped, onto a ramp
interpolated between two of Create's own copper (or brass) tones, which keeps every bolt and
panel line intact.

The flap is re-laid out: Create's bright horizontal band at row 5 is an item slot, which is
wrong for a fluid hatch, so it becomes a round pipe socket (bright lip, dark bore) in the
middle of the plate. The plate's existing bevel - dark top edge, bright bottom edge, because
the flap tilts - is kept.

Usage:
    python tools/create-fluid-hatch-textures.py <ccd-assets-root> <create-source> [--check|--verify]
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

COPPER = _cm.PAL_COPPER
BRASS = _cm.PAL_BRASS
N = 16


def _rgb(hexes):
    return [tuple(int(h[i : i + 2], 16) for i in (0, 2, 4)) for h in hexes]


# Wide ladders, all tones taken from Create, so a grey source keeps its contrast. Create's
# plain 8 step copper ramp only spans luminance 86..149, while hatch.png spans 45..183;
# tinting onto the narrow ramp halves the contrast and the housing goes flat.
COPPER_WIDE = _rgb([
    "3D161E",  # create:block/encased_pipe.png, the darkest copper seam
    "5B2924",  # create:block/encased_pipe.png
    "904931", "9A5038", "A75A40", "B26247", "C26B4C", "C87456", "D67B5B", "E3826C",
])
BRASS_WIDE = _rgb([
    "592424", "724731", "9E6947", "AF794F", "BF8E55", "C79758", "CEA05A",
    "D7AA5E", "E4B763", "EEC168", "F7CB6C", "FBDC7D", "FFEB8C",   # create:block/brass_block.png
])

TINT = {"copper": COPPER_WIDE, "brass": BRASS_WIDE}


def load_create(src: Path, rel: str) -> Image.Image:
    key = f"assets/create/textures/block/{rel}"
    if src.is_file():
        with zipfile.ZipFile(src) as zf:
            return Image.open(io.BytesIO(zf.read(key))).convert("RGBA")
    for c in (src / key, src / "block" / rel, src / rel):
        if c.exists():
            return Image.open(c).convert("RGBA")
    raise FileNotFoundError(rel)


def tint(src: Image.Image, ramp):
    """Remap luminance onto `ramp`, leaving alpha untouched."""
    px = src.load()
    lums = []
    for y in range(src.height):
        for x in range(src.width):
            r, g, b, a = px[x, y]
            if a:
                lums.append(0.2126 * r + 0.7152 * g + 0.0722 * b)
    lo, hi = min(lums), max(lums)
    span = (hi - lo) or 1.0
    out = src.copy()
    op = out.load()
    for y in range(src.height):
        for x in range(src.width):
            r, g, b, a = px[x, y]
            if not a:
                continue
            t = (0.2126 * r + 0.7152 * g + 0.0722 * b - lo) / span
            idx = max(0, min(len(ramp) - 1, round(t * (len(ramp) - 1))))
            op[x, y] = ramp[idx] + (a,)
    return out


# ---------------------------------------------------------------- front port
# The hatch's visible front is NOT the flap. A probe (paint key 0 magenta, key 1 cyan) shows
# the flap `#1` only on the slanted door's top face; the whole front panel is key `#0`, the
# housing atlas, sampled through the frame's north uv [1.5,1.5]-[7.5,8.5], i.e. x3..15 y3..17
# of the 32 px sheet.
#
# That window's interior is Create's louvred item door (x5..12, y5..14). For a fluid hatch it
# becomes a recessed port: bright surround, one pixel of lip, dark bore.
PORT = [
    "66666666",
    "61111116",
    "61000016",
    "61000016",
    "61000016",
    "61000016",
    "61000216",   # the bore's floor catches a little light, so it reads recessed
    "61111116",
    "66666666",
    "55555555",   # closes off the leftover louvre row underneath
]
PORT_X, PORT_Y = 5, 5


def stamp_port(im, palette, x0=PORT_X, y0=PORT_Y):
    px = im.load()
    for dy, row in enumerate(PORT):
        for dx, ch in enumerate(row):
            x, y = x0 + dx, y0 + dy
            if 0 <= x < im.width and 0 <= y < im.height:
                px[x, y] = palette[int(ch)] + (255,)
    return im


def derive(assets: Path, create_src: Path):
    housing_src = load_create(create_src, "hatch.png")
    flap_src = load_create(create_src, "hatch_closed.png")
    out = {}
    for material, key in (("fluid_hatch", "copper"), ("smart_fluid_hatch", "brass")):
        ramp = TINT[key]
        housing = tint(housing_src, ramp)
        stamp_port(housing, _cm.PAL_COPPER if key == "copper" else _cm.PAL_BRASS)
        out[f"block/{material}/housing.png"] = housing
        # The flap is only visible as the slanted door's top face, so a tint is enough.
        out[f"block/{material}/flap.png"] = tint(flap_src, ramp)
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

    built = derive(assets, create_src)

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
        print(f"all {len(built)} fluid hatch textures match")
        return 0

    print(f"{'texture':46s} {'size':7s} {'std':>5s} {'flat':>6s} {'tones':>5s}")
    for rel, im in sorted(built.items()):
        m = _cm.metrics(im)
        print(f"{rel:46s} {im.width}x{im.height:<4d} {m['std']:5.1f} {m['flat']:5.1f}% {m['tones']:5d}")
        if not check:
            path = assets / "textures" / rel
            path.parent.mkdir(parents=True, exist_ok=True)
            im.save(path)
    print("\ncheck only" if check else f"\n{len(built)} textures written")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
