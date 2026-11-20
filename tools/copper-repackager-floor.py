"""Change only the CCD repackager's interior floor to native copper casing."""
import json
from pathlib import Path
from zipfile import ZipFile

root = Path(__file__).resolve().parents[1] / 'src/main/resources/assets/ccd'
jar = next((Path.home() / '.gradle/caches/modules-2/files-2.1/com.simibubi.create/create-1.21.1/6.0.10-280').rglob('*-slim.jar'))
with ZipFile(jar) as archive:
    model = json.loads(archive.read('assets/create/models/block/repackager/block.json'))
model['textures']['copper_floor'] = 'create:block/copper_casing'
floor = next(e for e in model['elements'] if e['from'] == [0, 4, 0] and e['to'] == [16, 4, 16])
floor['name'] = 'copper_casing_interior_floor'
floor['faces']['up'] = {'uv': [0, 0, 16, 16], 'texture': '#copper_floor'}
model['render_type'] = 'minecraft:cutout_mipped'
model['ambientocclusion'] = False
(root / 'models/block/fluid_repackager/block.json').write_text(json.dumps(model, indent=2) + '\n')
