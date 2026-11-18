"""Finish the booster pump housing. Kept as an entry point for tools/create-booster-pump.cjs.

History: this script used to build the 16x16 body panel from a 6x6 PATCH by reflecting it
across both axes *and* the diagonal, then assert that symmetry:

    assert image.tobytes()==image.transpose(FLIP_LEFT_RIGHT).tobytes()
    assert image.tobytes()==image.transpose(ROTATE_90).tobytes()

A 16x16 face has 256 pixels; folding it twice left 10 free pixels, so the result read as
blur rather than sheet metal, and the assertion also forbade the top-left light direction
that Create's machine faces all use.

The body panel is now `create:block/copper_underside` referenced straight from the model, so
this script only rebuilds block/booster_pump/shaft_face (Create copper + the native gearbox
aperture) in tools/copper-material.py.

It no longer touches block/fluid_transmitter: those 32x32 faces carry a yellow indicator panel
and CCD arrow marks, and routing them through the shared copper plate dropped the indicator,
so they keep their own designed artwork.

Usage:
    python tools/symmetrize-fluid-housings.py [booster_pump] [create-source]
"""
from __future__ import annotations

import importlib.util
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/ccd"


def _load_material():
    spec = importlib.util.spec_from_file_location(
        "copper_material", Path(__file__).resolve().parent / "copper-material.py")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def find_create_source() -> Path | None:
    base = Path.home() / ".gradle/caches/modules-2/files-2.1/com.simibubi.create/create-1.21.1"
    if not base.exists():
        return None
    jars = sorted(base.rglob("create-*.jar"))
    full = [j for j in jars if not j.name.endswith("-slim.jar")]
    return (full or jars or [None])[0]


def main(argv):
    args = [a for a in argv[1:] if not a.startswith("--")]
    selected = args[0] if args and not args[0].endswith(".jar") and "/" not in args[0] else None
    explicit = next((a for a in args if a.endswith(".jar") or "/" in a or "\\" in a), None)
    source = Path(explicit).resolve() if explicit else find_create_source()
    if source is None:
        print("no Create source found; pass the Create jar or an extracted assets root")
        return 2

    cm = _load_material()
    built = cm.derive(ASSETS, source)
    wanted = {
        "booster_pump": ["block/booster_pump/shaft_face.png"],
    }
    written = 0
    for name, rels in wanted.items():
        if selected and selected != name:
            continue
        for rel in rels:
            im = built.get(rel)
            if im is None:
                print(f"skip {rel} (source texture missing)")
                continue
            im.save(ASSETS / "textures" / rel)
            written += 1
            m, s = cm.metrics(im), cm.symmetry(im)
            assert max(s.values()) > 5.0, f"{rel} came out rotationally symmetric"
            print(f"{rel:42s} std={m['std']:5.1f} flat={m['flat']:5.1f}% tones={m['tones']:2d} "
                  f"sym LR/UD/R180/R90={s['LR']}/{s['UD']}/{s['R180']}/{s['R90']}")
    print(f"\n{written} texture(s) rebuilt from Create copper; no mirror symmetry enforced")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
