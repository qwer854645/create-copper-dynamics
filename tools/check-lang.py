"""Lang file guard for CCD: key parity, emphasis markers, placeholders, tooltip structure.

Run before committing any change to `assets/ccd/lang/`:

    python tools/check-lang.py

Exit code 0 means the files agree with each other and with docs/text-voice.md.
"""
from __future__ import annotations

import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
LANG = ROOT / "src/main/resources/assets/ccd/lang"
LANGS = ("en_us", "zh_cn")
PLACEHOLDER = re.compile(r"%(?:\d+\$)?[sd]")
PAIR = re.compile(r"^(?P<base>.+\.tooltip)\.(condition|behaviour|control|action)(?P<n>\d+)$")


def load(name):
    path = LANG / f"{name}.json"
    with path.open(encoding="utf-8") as fh:
        return json.load(fh)


def main() -> int:
    data = {name: load(name) for name in LANGS}
    problems: list[str] = []
    notes: list[str] = []

    # 1. key parity
    en, zh = data["en_us"], data["zh_cn"]
    for key in sorted(set(en) ^ set(zh)):
        problems.append(f"key only in one language: {key}")

    for key in sorted(set(en) & set(zh)):
        a, b = en[key], zh[key]
        if not a.strip() or not b.strip():
            problems.append(f"empty value: {key}")
        # 2. emphasis markers must pair up
        for lang, text in (("en", a), ("zh", b)):
            if text.count("_") % 2:
                problems.append(f"unbalanced '_' ({lang}): {key}")
        # 3. placeholders must match one-for-one
        pa, pb = PLACEHOLDER.findall(a), PLACEHOLDER.findall(b)
        if sorted(pa) != sorted(pb):
            problems.append(f"placeholder mismatch: {key} en={pa} zh={pb}")
        # 4. no trailing period on conditions/controls
        m = PAIR.match(key)
        if m and m.group(2) in ("condition", "control"):
            for lang, text in (("en", a), ("zh", b)):
                if text.rstrip().endswith((".", "。")):
                    problems.append(f"condition/control ends with a period ({lang}): {key}")

    # 5. tooltip structure: every condition/control needs its counterpart
    for key in en:
        m = PAIR.match(key)
        if not m:
            continue
        kind, n, base = m.group(2), m.group(3), m.group("base")
        counterpart = ("behaviour" if kind == "condition" else "action") if kind in (
            "condition", "control") else None
        if counterpart is None:
            continue
        partner = f"{base}.{counterpart}{n}"
        if partner not in en:
            problems.append(f"{key} has no {counterpart}{n}")

    # 6. ponder scenes: header present, text_n numbering contiguous from 1
    scenes: dict[str, list[int]] = {}
    for key in en:
        m = re.match(r"^ccd\.ponder\.(?P<scene>[a-z_]+)\.(?P<field>header|text_(?P<n>\d+))$", key)
        if not m:
            continue
        scenes.setdefault(m.group("scene"), [])
        if m.group("n"):
            scenes[m.group("scene")].append(int(m.group("n")))
    for scene, nums in sorted(scenes.items()):
        if f"ccd.ponder.{scene}.header" not in en:
            problems.append(f"ponder scene without header: {scene}")
        if nums != list(range(1, len(nums) + 1)):
            problems.append(f"ponder text numbering has a gap: {scene} -> {sorted(nums)}")

    # 7. informational: length budget from docs/text-voice.md
    for key, limit in ((k, 80) for k in en if k.endswith(".tooltip.summary")):
        if len(en[key]) > limit:
            notes.append(f"long summary (>{limit} chars): {key} ({len(en[key])})")

    print(f"lang: {len(en)} keys per language, {len(scenes)} ponder scenes")
    for note in notes:
        print(f"note: {note}")
    if problems:
        print()
        for p in problems:
            print(f"FAIL {p}")
        print(f"\n{len(problems)} problem(s)")
        return 1
    print("RESULT: OK")
    return 0


if __name__ == "__main__":
    sys.exit(main())
