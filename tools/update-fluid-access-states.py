"""Split CCD's current casing/socket models for live connection feedback.

Reads existing CCD artwork; never generates or changes textures.
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / 'src/main/resources/assets/ccd'


def save(path, value):
    path.write_text(json.dumps(value, indent=2) + '\n', encoding='utf-8')


for name in ('fluid_access_port', 'fluid_bridge'):
    original = json.loads((ROOT / f'models/block/{name}.json').read_text())
    for suffix, elements in (
        ('casing', [e for e in original['elements'] if e['name'] == 'casing']),
        ('socket', [e for e in original['elements'] if e['name'] == 'raised_socket_south']),
    ):
        save(ROOT / f'models/block/{name}_{suffix}.json', {**original, 'elements': elements})
    parts = []
    rotations = {'north': (0, 0), 'south': (0, 180), 'east': (0, 90),
                 'west': (0, 270), 'up': (270, 0), 'down': (90, 0)}
    opposite = {'north': 'south', 'south': 'north', 'east': 'west', 'west': 'east', 'up': 'down', 'down': 'up'}
    for facing, (x, y) in rotations.items():
        parts.append({'when': {'facing': facing}, 'apply': {'model': f'ccd:block/{name}_casing', 'x': x, 'y': y}})
        ends = [('attached_negative', opposite[facing])]
        if name == 'fluid_bridge':
            negative = 'down' if facing in ('up', 'down') else 'west' if facing in ('east', 'west') else 'north'
            ends = [('attached_negative', negative), ('attached_positive', opposite[negative])]
        for flag, end in ends:
            # The unrotated south socket points opposite the model's FACING.
            sx, sy = rotations[opposite[end]]
            parts.append({'when': {'facing': facing, flag: True},
                          'apply': {'model': f'ccd:block/{name}_socket', 'x': sx, 'y': sy}})
    save(ROOT / f'blockstates/{name}.json', {'multipart': parts})
