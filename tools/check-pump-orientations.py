"""Check baked pump ports/shaft holes against runtime direction conventions."""
import json
from pathlib import Path

root = Path(__file__).resolve().parents[1] / 'src/main/resources/assets/ccd'
vectors = {'east': (1, 0, 0), 'west': (-1, 0, 0), 'up': (0, 1, 0),
           'down': (0, -1, 0), 'south': (0, 0, 1), 'north': (0, 0, -1)}
def clockwise(v, axis):
    x, y, z = v
    return (x, z, -y) if axis == 0 else (-z, y, x) if axis == 1 else (y, -x, z)
def negate(v): return tuple(-n for n in v)
def name(v): return next(k for k, value in vectors.items() if value == v)

for pump in ('turning_booster_pump', 'bidirectional_fluid_pump'):
    states = json.loads((root / f'blockstates/{pump}.json').read_text())['variants']
    assert len(states) == 48
    for key, spec in states.items():
        state = dict(pair.split('=') for pair in key.split(','))
        front = vectors[state['facing']]
        axis = next(i for i, n in enumerate(front) if n)
        if pump == 'turning_booster_pump':
            other = vectors['north'] if axis == 1 else vectors['up']
        else:
            other = vectors['east'] if axis == 2 else vectors['south']
        for _ in range(int(state['roll'])): other = clockwise(other, axis)
        if pump == 'bidirectional_fluid_pump' and state['roll'] == '0': assert other[1] == 0
        model = json.loads((root / ('models/' + spec['model'].split(':')[1] + '.json')).read_text())
        holes = {side for side, face in model['elements'][0]['faces'].items() if face['texture'] == '#shaft_face'}
        expected = {name(negate(front)), name(negate(other))} if pump == 'turning_booster_pump' else {name(other), name(negate(other))}
        assert holes == expected, (pump, key, holes, expected)
        ports = {name(front), name(other)} if pump == 'turning_booster_pump' else {name(front), name(negate(front))}
        actual_ports = set()
        for element in model['elements'][1:]:
            if 'rim' not in element.get('name', ''): continue
            for side, vector in vectors.items():
                coordinate = next(i for i, n in enumerate(vector) if n)
                boundary = 16 if vector[coordinate] > 0 else 0
                if boundary in (element['from'][coordinate], element['to'][coordinate]): actual_ports.add(side)
        assert actual_ports == ports, (pump, key, actual_ports, ports)
    print(f'{pump}: all 48 states have matching pipe mouths and shaft holes')
