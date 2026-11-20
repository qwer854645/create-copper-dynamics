"""Keep the rotor shaft behind the front blade plane, including the item model."""
import json
from pathlib import Path
root = Path(__file__).resolve().parents[1] / 'src/main/resources/assets/ccd/models'
for relative in ('block/fluid_atomizer/propeller.json', 'item/fluid_atomizer.json'):
    path = root / relative
    model = json.loads(path.read_text())
    shaft = next(e for e in model['elements'] if e.get('name') == 'Shaft')
    shaft['from'][2] = 4.25
    length = shaft['to'][2] - 4.25
    for side in ('east', 'west', 'up', 'down'):
        face = shaft['faces'][side]
        face['uv'][3] = face['uv'][1] + length
    path.write_text(json.dumps(model, indent=2) + '\n')
