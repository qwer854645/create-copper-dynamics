"""Import the user atlas; its left/right red strips provide the on/off palettes."""
from pathlib import Path
import json
import shutil
from PIL import Image

root = Path(__file__).resolve().parents[1]
assets = root / 'src/main/resources/assets/ccd'
source = Path(r'C:\Users\Administrator\Downloads\body.png')
backup = root / 'outputs/smart-grate-indicators/backup'
backup.mkdir(parents=True, exist_ok=True)
texture = assets / 'textures/block/smart_collection_grate/body.png'
shutil.copy2(texture, backup / 'body.png')
shutil.copy2(source, backup.parent / 'user-body.png')
atlas = Image.open(source).convert('RGBA')
assert atlas.size == (16, 16)
off = atlas.copy()
on = atlas.copy()
for y in range(2, 7):
    for dx in range(2):
        # Only the two 2x5 indicator windows change. Keep all surrounding art exact.
        off.putpixel((2 + dx, y), atlas.getpixel((12 + dx, y)))
        on.putpixel((12 + dx, y), atlas.getpixel((2 + dx, y)))
off.save(texture)
on.save(texture.with_name('body_powered.png'))
model_dir = assets / 'models/block'
(model_dir / 'smart_collection_grate_powered.json').write_text(json.dumps({
    'parent': 'ccd:block/smart_collection_grate',
    'textures': {'6': 'ccd:block/smart_collection_grate/body_powered'}
}, indent=2) + '\n', encoding='utf-8')
state = assets / 'blockstates/smart_collection_grate.json'
shutil.copy2(state, backup / state.name)
state.write_text(json.dumps({'multipart': [
    {'when': {'powered': False}, 'apply': {'model': 'ccd:block/smart_collection_grate'}},
    {'when': {'powered': True}, 'apply': {'model': 'ccd:block/smart_collection_grate_powered'}}
]}, indent=2) + '\n', encoding='utf-8')
assert all(off.getpixel((x,y)) == on.getpixel((x,y)) for y in range(16) for x in range(16)
           if not (x in (2,3,12,13) and 2 <= y < 7))
print('Imported atlas; both indicators dark without signal, bright with signal.')
