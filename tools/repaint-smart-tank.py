"""Smart tank brass repaint - thin wrapper around tools/recolor-rank.py.

This script used to duplicate the recolour loop with its own absolute-threshold ladder:

    index = next((i for i, t in enumerate([35, 55, 75, 90, 108, 128, 150, 180])
                  if luminance < t), 8)

which mapped Create's eight copper tones (lum 86.4..149.0) onto indices
[3, 4, 4, 5, 5, 6, 6, 6]: four distinct outputs, all lifted by 70..100 luminance. It also
imported `pxl.Canvas` from a local pixel-art skill that is not part of this repository, so
it could not run here at all, and it repainted every `*.png` in the folder rather than only
the fluid tank faces.

The logic now lives in one place. This wrapper keeps the old entry point working and
restricts it to the four tank body textures handled by tools/recolor-rank.py.
"""
from __future__ import annotations

import importlib.util
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def _load(name, filename):
    spec = importlib.util.spec_from_file_location(name, Path(__file__).resolve().parent / filename)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def main(argv):
    rank = _load("recolor_rank", "recolor-rank.py")
    positional = [a for a in argv[1:] if not a.startswith("--")]
    source = positional[0] if positional else None
    if source is None:
        # same auto-discovery the old script used
        base = Path.home() / ".gradle/caches/modules-2/files-2.1/com.simibubi.create/create-1.21.1"
        jars = sorted(base.rglob("create-*.jar")) if base.exists() else []
        jars = [j for j in jars if not j.name.endswith("-slim.jar")] + jars
        if jars:
            source = str(jars[0])
    if not source:
        print("no Create jar given and none found in the Gradle cache; "
              "pass one: python tools/repaint-smart-tank.py <create.jar>")
        return 2
    args = ["recolor-rank.py", source]
    if "--check" in argv:
        args.append("--check")
    return rank.main(args)


if __name__ == "__main__":
    sys.exit(main(sys.argv))
