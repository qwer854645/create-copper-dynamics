"""Deepen existing collection grate models without regenerating their textures.

Idempotent migration from the old eight-pixel bowl to a sixteen-pixel bowl.
The native bottom pipe, floor, lip thickness and bar thickness stay intact.
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MODELS = ROOT / 'src/main/resources/assets/ccd/models'


def resize(path):
    data = json.loads(path.read_text(encoding='utf-8'))
    changed = False
    for element in data.get('elements', []):
        name = element.get('name', '')
        old_bottom, old_top = element['from'][1], element['to'][1]
        if name.startswith('lower_bowl') and (old_bottom, old_top) == (3, 5):
            element['to'][1] = 9
        elif name.startswith('upper_bowl') and (old_bottom, old_top) == (5, 7):
            element['from'][1], element['to'][1] = 9, 15
        elif name.startswith('raised_copper_lip') and (old_bottom, old_top) == (7, 8):
            element['from'][1], element['to'][1] = 15, 16
        elif name.startswith('recessed_grate') and (old_bottom, old_top) == (6, 7):
            element['from'][1], element['to'][1] = 14, 15
        elif name == 'front_filter_panel' and (old_bottom, old_top) == (2, 7):
            element['from'][1], element['to'][1] = 8, 13
        else:
            continue
        # Give deeper side walls one texel per model pixel; preserve all top,
        # rim, bar, filter and pipe artwork and their existing UVs.
        if name.startswith(('lower_bowl', 'upper_bowl')):
            for side in ('north', 'south', 'east', 'west'):
                element['faces'][side]['uv'][3] = element['faces'][side]['uv'][1] + 6
        changed = True
    if changed:
        path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
        print(path.relative_to(ROOT))


if __name__ == '__main__':
    for relative in ('block/copper_collection_grate.json', 'block/smart_collection_grate.json',
                     'block/smart_collection_grate_filter.json', 'item/smart_collection_grate.json'):
        resize(MODELS / relative)
