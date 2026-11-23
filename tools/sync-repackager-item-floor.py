"""Match the CCD repackager item floor and tray to its world models.

Geometry and other UVs come unchanged from Create 6.0.10-280.
"""
from pathlib import Path
from zipfile import ZipFile
import json
import importlib.util
from PIL import Image, ImageDraw

root = Path(__file__).resolve().parents[1]
jar = next((Path.home()/'.gradle/caches/modules-2/files-2.1/com.simibubi.create/create-1.21.1/6.0.10-280').rglob('*-slim.jar'))
with ZipFile(jar) as archive:
    model = json.loads(archive.read('assets/create/models/block/repackager/item.json'))
model['render_type'] = 'minecraft:cutout_mipped'
model['textures']['copper_floor'] = 'create:block/copper_casing'
changed = 0
for element in model['elements']:
    if element['from'] == [2, 2, 2] and element['to'] == [14, 4, 14] or element['from'] == [0, 4, 0] and element['to'] == [16, 4, 16]:
        element['faces']['up'] = {'uv': [0, 0, 16, 16], 'texture': '#copper_floor'}
        changed += 1
assert changed == 2
path = root/'src/main/resources/assets/ccd/models/item/fluid_repackager.json'
path.write_text(json.dumps(model, indent=2)+'\n', encoding='utf-8')

spec = importlib.util.spec_from_file_location('preview', root/'tools/preview-smart-grate-indicators.py')
p = importlib.util.module_from_spec(spec)
spec.loader.exec_module(p)
out = root/'outputs/repackager-item-floor'
out.mkdir(parents=True, exist_ok=True)
image = Image.new('RGBA', (960, 520), '#e5e8e2')
image.paste(p.render('create:block/repackager/item'), (0, 30))
image.paste(p.render('ccd:item/fluid_repackager'), (480, 30))
draw = ImageDraw.Draw(image)
draw.text((20, 10), 'Before: native item floor', fill='#303535')
draw.text((500, 10), 'After: CCD copper floor and tray', fill='#303535')
image.save(out/'comparison.png')
print('Updated item floor and tray; geometry and remaining face UVs preserved.')
