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

# The body ramps are a *slice of the same ladder*, not a different palette: every tone below is
# a real Create colour. Where the slice starts sets how dark the hatch looks, and the number of
# steps sets how much of the source's shading survives.
#
#   copper start #3D161E (lum 31, 10 steps) -> "too dark"
#   copper start #9A5038 (lum 94,  7 steps) -> "too pale": the slice was too short and went flat
#   copper start #724731 (lum 79,  9 steps) -> still "too dark" once the repainting was removed
#   copper start #9A5038 (lum 94,  7 steps) -> current; with nothing painted on top, the short
#                                              slice no longer flattens anything
COPPER_BODY = _rgb(["9A5038", "A75A40", "B26247", "C26B4C", "C87456", "D67B5B", "E3826C"])

# Brass follows create:block/mechanical_arm.png rather than brass_block (user: "榛勯摐鍙互鍙傝€冨姩鍔涜噦
# 鐨勯鑹?). The arm carries more intermediate bronze tones between lum 78 and 116 - #854A34,
# #875137, #8E5B3D, #995A3D, #A46B47 - where brass_block jumps straight from #724731 to #9E6947.
# Those extra steps are what make it read as machined brass instead of a flat gold sheet.
BRASS_BODY = _rgb(["8E5B3D", "995A3D", "9E6947", "A46B47", "AF794F", "BF8E55", "C79758",
                   "CEA05A", "D7AA5E", "E4B763", "EEC168", "F7CB6C"])

TINT = {"copper": COPPER_BODY, "brass": BRASS_BODY}


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


# ---------------------------------------------------------------- material swap
# Create's hatch art is three distinct materials, not one grey ramp:
#   lum <= 56    the black recesses            (11% of the atlas)
#   56 < lum < 145  the industrial iron body   (78%)
#   lum >= 145   the bare silver highlights    (8%)
# Remapping all of it onto one copper ramp is what made it look like a photo filter.
# Only the middle band is swapped for copper/brass; the blacks and the silver hardware are
# left exactly as Create drew them, which is also how Create does its own brass machines.
BLACK_MAX = 56.0
SILVER_MIN = 145.0


def swap_material(src: Image.Image, ramp, keep_silver=True, keep_black=True, warm_black=None):
    px = src.load()
    out = src.copy()
    op = out.load()
    span = (SILVER_MIN - BLACK_MAX) or 1.0
    steps = len(ramp) - 1
    for y in range(src.height):
        for x in range(src.width):
            r, g, b, a = px[x, y]
            if not a:
                continue
            L = 0.2126 * r + 0.7152 * g + 0.0722 * b
            if L <= BLACK_MAX:
                if keep_black:
                    continue
                if warm_black is not None:
                    t = L / BLACK_MAX
                    idx = max(0, min(len(warm_black) - 1, round(t * (len(warm_black) - 1))))
                    op[x, y] = warm_black[idx] + (a,)
                continue
            if L >= SILVER_MIN:
                if keep_silver:
                    continue
                continue
            t = (L - BLACK_MAX) / span
            op[x, y] = ramp[max(0, min(steps, round(t * steps)))] + (a,)
    return out


# ---------------------------------------------------------------- why there is no port
# The hatch's visible front is NOT the flap. A probe (paint key 0 magenta, key 1 cyan) shows
# the flap `#1` only on the slanted door; the whole front panel is key `#0`, the housing
# atlas, sampled through the frame's north uv [1.5,1.5]-[7.5,8.5], i.e. x3..15 y3..17 of the
# 32 px sheet.
#
# An earlier revision stamped a recessed pipe port into x5..12/y5..14 of that window. It was
# dropped: that window is Create's louvred item door, and a stamped hole destroyed the louvre
# and read as a hole cut through a copper plate. Material-swapping the body and keeping the
# louvre, the blacks and the silver reads far better, and a fluid hatch that keeps the hatch
# form is what reusing this model is asking for.
#
# If a stronger fluid cue is ever wanted, put a small droplet badge in a corner instead of
# replacing the door.

# The two visible windows of the 32 px atlas:
# The two windows of the 32 px atlas, kept here because the coordinates are easy to get wrong:
#
#   FRAME_WIN  element 1 (the protruding frame) north face  uv [1.5,1.5]-[7.5,8.5] -> x3..15 y3..17
#   WALL_WIN   element 0 (the wall) north *and* south faces uv [1,1]-[8,9]        -> x2..15 y2..17
#              (x16 / y18 are transparent in Create's sheet, hence 15..17)
#
# The frame side ends up against the container; WALL_WIN is the face players see. Earlier revisions
# repainted these windows (a bezel, then a slate recess). That rearranged Create's artwork and read
# as busy on the front and muddy on the back, so nothing here touches pixel layout any more: the
# frame contrast comes from the ramp plus the rim and seams Create already drew.



# The arm's dark end (#724731, #854A34, #875137) is not used: on this hatch it made the body read
# too dark, because Create's hatch body carries more low-mid tones than the arm's flat panels do.
# The slice now starts at #8E5B3D.
# ---------------------------------------------------------------- matching Create's brass values
# Measured over every warm pixel:
#
#   create:block/brass_block      p50 174   frac<110  11%
#   create:block/mechanical_arm   p50 174   frac<110  18%
#   create:block/hatch  (source)  p50  79   frac<110  87%
#
# The item hatch is a DARK grey object and Create's brass parts are bright, so any linear grey->brass
# ramp keeps the hatch dark: that is why every earlier attempt still read "too many dark tones".
# The fix is to match the value DISTRIBUTION instead of the palette - each body pixel takes the
# reference colour at its own quantile. Ordering is preserved (the map is monotone), so every edge,
# slat and seam stays exactly where Create drew it.
BRASS_REFERENCE = ("block/mechanical_arm", "block/brass_block")


def _warm(c):
    r, g, b = c[0], c[1], c[2]
    return r >= g >= b and (r - b) >= 18 and (g - b) >= 6


def _lum(c):
    return 0.2126 * c[0] + 0.7152 * c[1] + 0.0722 * c[2]


def build_reference(create_src: Path, keys=BRASS_REFERENCE):
    """Every warm pixel of the reference art, sorted - frequencies count, not just unique tones."""
    out = []
    for key in keys:
        im = load_create(create_src, key.split("/", 1)[1] + ".png")
        out += [c[:3] for c in im.getdata() if c[3] and _warm(c)]
    out.sort(key=_lum)
    return out


def match_distribution(src: Image.Image, reference):
    """Monotone remap of the body onto a reference value distribution."""
    px = src.load()
    out = src.copy()
    op = out.load()
    body = [(x, y) for y in range(src.height) for x in range(src.width)
            if px[x, y][3] and BLACK_MAX < _lum(px[x, y][:3]) < SILVER_MIN]
    if not body or not reference:
        return out
    ordered = sorted(body, key=lambda p: _lum(px[p[0], p[1]][:3]))
    n, m = len(ordered), len(reference)
    for i, (x, y) in enumerate(ordered):
        q = i / (n - 1) if n > 1 else 0.5
        op[x, y] = reference[min(m - 1, int(q * m))] + (px[x, y][3],)
    return out


def derive(assets: Path, create_src: Path):
    """Create's pixel layout is kept exactly; only the grey body tones move.

    Copper takes a straight palette slice. Brass is matched to the value distribution of
    create:block/mechanical_arm + create:block/brass_block, because a straight slice keeps the
    hatch's own dark value histogram (median 79) and reads muddy no matter which brass tones are
    used. Neither path repaints, moves or adds a pixel.
    """
    housing_src = load_create(create_src, "hatch.png")
    flap_src = load_create(create_src, "hatch_closed.png")
    reference = build_reference(create_src)
    out = {}
    for material, key in (("fluid_hatch", "copper"), ("smart_fluid_hatch", "brass")):
        ramp = TINT[key]
        if key == "brass":
            out[f"block/{material}/housing.png"] = match_distribution(housing_src, reference)
            out[f"block/{material}/flap.png"] = match_distribution(flap_src, reference)
        else:
            out[f"block/{material}/housing.png"] = swap_material(housing_src, ramp)
            out[f"block/{material}/flap.png"] = swap_material(flap_src, ramp)
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

