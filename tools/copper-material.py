"""The few CCD face textures that cannot simply be a `create:block/...` reference.

Everything that was only "a copper plate" now points straight at Create in the models
(`create:block/copper_underside`, `create:block/brass_block`,
`create:block/industrial_iron_block`) - see the swap table in docs/texture-rework-plan.md.
That removed the hand-drawn material that read as flat plastic, and it cannot drift when
Create updates.

Four textures still have to exist because their *pixel layout* carries meaning:

    block/booster_pump/shaft_face        Create copper_underside + the native gearbox aperture
    block/smart_collection_grate/body    Create brass_block 2x2 (32x32 face, brass)
    block/copper_water_supply/cap        concentric recess, drawn on Create's copper ramp
    block/fluid_arm/core                 dark shaft recess, drawn on a dark ramp

All four are built *from* Create's own pixels or from Create's own eight copper tones; none
of them introduces a new palette.

NOTE: block/fluid_transmitter/{body,side,top} are deliberately NOT generated here. Those
32x32 faces carry a yellow indicator panel plus CCD arrow marks; tiling copper_underside over
them dropped the indicator and put the arrow on a tiling seam, which read as a stray cross.
They stay as the original hand-designed textures.

Usage:
    python tools/copper-material.py <ccd-assets-root> <create-source> [--check|--verify|--preview <png>]

<create-source> is the Create jar or an extracted assets root.
Only Python + Pillow are required.
"""
from __future__ import annotations

import importlib.util
import io
import math
import sys
import zipfile
from collections import Counter
from pathlib import Path

from PIL import Image

# Create's 8 copper tones, verbatim from create:block/copper_underside + fluid_tank.
COPPER = ["904931", "9A5038", "A75A40", "B26247", "C26B4C", "C87456", "D67B5B", "E3826C"]
# 8 step brass ladder with the same luminance span as the copper one, taken from
# create:block/brass_block. The narrower ladder is only used for palette swaps; plain
# brass faces reference create:block/brass_block directly.
BRASS = ["724731", "7F4E2B", "8C5834", "9E6947", "AF794F", "BF8E55", "CEA05A", "D7AA5E"]
# Dark recess ramp derived from the existing fluid_arm/core.png hue.
DARK = ["10181A", "181F21", "20282A", "283033", "31393C", "3A4346"]


def to_rgb(hexes):
    return [tuple(int(h[i : i + 2], 16) for i in (0, 2, 4)) for h in hexes]


PAL_COPPER = to_rgb(COPPER)
PAL_BRASS = to_rgb(BRASS)
PAL_DARK = to_rgb(DARK)
PALETTES = {"copper": PAL_COPPER, "brass": PAL_BRASS, "dark": PAL_DARK}


# ---------------------------------------------------------------- metrics
def metrics(im):
    px = [im.getpixel((x, y)) for y in range(im.height) for x in range(im.width)]
    ls = [0.2126 * c[0] + 0.7152 * c[1] + 0.0722 * c[2] for c in px]
    mean = sum(ls) / len(ls)
    std = math.sqrt(sum((v - mean) ** 2 for v in ls) / len(ls))
    cnt = Counter(px)
    edges = 0
    for y in range(im.height):
        for x in range(im.width):
            if x + 1 < im.width and im.getpixel((x, y)) != im.getpixel((x + 1, y)):
                edges += 1
            if y + 1 < im.height and im.getpixel((x, y)) != im.getpixel((x, y + 1)):
                edges += 1
    return {"std": round(std, 1), "flat": round(cnt.most_common(1)[0][1] / len(px) * 100, 1),
            "tones": len(cnt), "edges": edges}


def symmetry(im):
    n = im.width
    px = im.load()
    out = {}
    for name, fn in (("LR", lambda x, y: (n - 1 - x, y)), ("UD", lambda x, y: (x, n - 1 - y)),
                     ("R180", lambda x, y: (n - 1 - x, n - 1 - y)), ("R90", lambda x, y: (y, n - 1 - x))):
        diff = sum(1 for y in range(n) for x in range(n) if px[x, y] != px[fn(x, y)[0], fn(x, y)[1]])
        out[name] = round(diff / (n * n) * 100, 1)
    return out


# ---------------------------------------------------------------- Create source
def load_create(src: Path, rel: str) -> Image.Image:
    """Read `assets/create/textures/block/<rel>` from a jar or an extracted root."""
    key = f"assets/create/textures/block/{rel}"
    if src.is_file():
        with zipfile.ZipFile(src) as zf:
            return Image.open(io.BytesIO(zf.read(key))).convert("RGBA")
    for candidate in (src / key, src / "block" / rel, src / rel):
        if candidate.exists():
            return Image.open(candidate).convert("RGBA")
    raise FileNotFoundError(f"{rel} not found under {src}")


def tile(im: Image.Image, times: int = 2) -> Image.Image:
    n = im.width
    out = Image.new("RGBA", (n * times, n * times))
    for ty in range(times):
        for tx in range(times):
            out.paste(im, (tx * n, ty * n))
    return out


# ---------------------------------------------------------------- derivations
def already_relit(im: Image.Image, palette) -> bool:
    """True when every pixel is already one of `palette`'s tones (kept for the sanity check)."""
    allowed = {c + (255,) for c in palette}
    return all(px in allowed for px in im.getdata())


def cap(n=16):
    """Concentric recessed cover, lit from the top-left, drawn straight onto Create's copper.

    Generated from ring geometry rather than from a stored bitmap, so it is deterministic and
    a second run produces exactly the same pixels.
    """
    g = [[4] * n for _ in range(n)]
    for y in range(n):
        for x in range(n):
            ring = min(x, y, n - 1 - x, n - 1 - y)
            lit = (x + y) < (n - 1)
            if ring == 0:
                g[y][x] = 2 if lit else 0
            elif ring == 1:
                g[y][x] = 6 if lit else 3
            elif ring == 2:
                g[y][x] = 1
            else:
                v = 4
                if lit:
                    v += 1
                if 5 <= x <= 10 and 5 <= y <= 10:
                    v = 5
                if 7 <= x <= 8 and 7 <= y <= 8:
                    v = 6
                g[y][x] = min(6, v)
    return _to_image(g, PAL_COPPER)


def core(n=16):
    """Dark shaft recess: a bevelled dark plate, drawn straight onto the dark ramp."""
    g = [[2] * n for _ in range(n)]
    for y in range(n):
        for x in range(n):
            if x == 0 or y == 0:
                g[y][x] = 4
            elif x == n - 1 or y == n - 1:
                g[y][x] = 1
            elif x == 1 or y == 1:
                g[y][x] = 0
            elif x == n - 2 or y == n - 2:
                g[y][x] = 1
            else:
                g[y][x] = 3 if (x + y) < n - 2 else 2
    return _to_image(g, PAL_DARK)


def _to_image(g, palette):
    n = len(g)
    im = Image.new("RGBA", (n, n))
    for y in range(n):
        for x in range(n):
            im.putpixel((x, y), palette[max(0, min(len(palette) - 1, g[y][x]))] + (255,))
    return im


def relight(src: Image.Image, palette, keep_shape=0.55, light=0.55, bevel=1):
    """Keep a texture's silhouette, re-light it onto `palette` without inventing new tones."""
    n = src.width
    px = src.load()
    lum = [[0.2126 * px[x, y][0] + 0.7152 * px[x, y][1] + 0.0722 * px[x, y][2]
            for x in range(n)] for y in range(n)]
    flat = [v for row in lum for v in row]
    lo, hi = min(flat), max(flat)
    span = (hi - lo) or 1.0
    steps = len(palette) - 1
    out = Image.new("RGBA", (n, n))
    for y in range(n):
        for x in range(n):
            shape = (lum[y][x] - lo) / span
            d = 1.0 - (0.5 * x / (n - 1) + 0.5 * y / (n - 1))
            idx = round(max(0.0, min(1.0, keep_shape * shape + light * d)) * steps)
            if bevel:
                if y == 0 or x == 0:
                    idx = min(steps, idx + bevel)
                elif y == n - 1 or x == n - 1:
                    idx = max(0, idx - bevel)
            out.putpixel((x, y), palette[idx] + (255,))
    return out


def derive(assets: Path, create_src: Path):
    """Return {relative path: PIL image} for the four textures that must exist."""
    tex = assets / "textures"
    copper_underside = load_create(create_src, "copper_underside.png")
    brass_block = load_create(create_src, "brass_block.png")

    out = {}

    # Create copper with the native gearbox aperture kept in the centre. The existing file
    # already holds that aperture at (4,4)-(12,12); reuse it verbatim, as the generate step
    # always did.
    shaft_path = tex / "block/booster_pump/shaft_face.png"
    if shaft_path.exists():
        old = Image.open(shaft_path).convert("RGBA")
        face = copper_underside.copy()
        face.paste(old.crop((4, 4, 12, 12)), (4, 4))
        out["block/booster_pump/shaft_face.png"] = face

    # 32 px brass body: Create brass tiled 2x2, which reads as four joined brass plates.
    out["block/smart_collection_grate/body.png"] = tile(brass_block, 2)

    # Concentric cap and dark recess: geometric, so they are deterministic across runs.
    out["block/copper_water_supply/cap.png"] = cap(16)
    out["block/fluid_arm/core.png"] = core(16)

    return out


# ---------------------------------------------------------------- CLI
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
    preview = None
    if "--preview" in argv:
        preview = Path(argv[argv.index("--preview") + 1]).resolve()

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
        print(f"all {len(built)} Create-derived textures match")
        return 0

    print(f"{'texture':42s} {'size':6s} {'std':>5s} {'flat':>6s} {'tones':>5s}  symmetry LR/UD/R180/R90")
    for rel, im in sorted(built.items()):
        m, s = metrics(im), symmetry(im)
        print(f"{rel:42s} {im.width}x{im.height:<3d} {m['std']:5.1f} {m['flat']:5.1f}% {m['tones']:5d}  "
              f"{s['LR']:5.1f} {s['UD']:5.1f} {s['R180']:5.1f} {s['R90']:5.1f}")
        if not check:
            path = assets / "textures" / rel
            if path.exists():
                old = Image.open(path).convert("RGBA")
                if old.size != im.size:
                    raise SystemExit(f"size mismatch for {rel}: {old.size} -> {im.size}")
                if any(a != 255 for a in old.getchannel("A").getdata()):
                    raise SystemExit(f"refusing to flatten alpha of {rel}")
            path.parent.mkdir(parents=True, exist_ok=True)
            im.save(path)

    if preview:
        make_preview(preview, built)
    print("\ncheck only, nothing written" if check else f"\n{len(built)} textures written")
    return 0


def make_preview(out_path: Path, built):
    from PIL import ImageDraw

    items = sorted(built.items())
    zoom = 14
    cw = max(im.width for _, im in items) * zoom
    cols = 4
    lab = 26
    rows = (len(items) + cols - 1) // cols
    sheet = Image.new("RGBA", (cols * (cw + 14) + 14, rows * (cw + lab + 14) + 14), (24, 26, 31, 255))
    d = ImageDraw.Draw(sheet)
    for i, (name, im) in enumerate(items):
        r, c = divmod(i, cols)
        x = 14 + c * (cw + 14)
        y = 14 + r * (cw + lab + 14)
        k = cw // max(im.width, im.height)
        sheet.alpha_composite(im.resize((im.width * k, im.height * k), Image.NEAREST), (x, y))
        d.text((x, y + im.height * k + 4), name.split("/")[-1], fill=(222, 227, 236, 255))
    out_path.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(out_path)
    print(f"preview -> {out_path}")


if __name__ == "__main__":
    sys.exit(main(sys.argv))
