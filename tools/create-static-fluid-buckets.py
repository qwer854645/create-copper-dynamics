"""Bake single-frame bucket icons; no fluid-container loader or animated item textures."""
import json
from io import BytesIO
from pathlib import Path
from zipfile import ZipFile
from PIL import Image
ROOT=Path(__file__).resolve().parents[1]
A=ROOT/'src/main/resources/assets/ccd'
colors={'quicksand':(211,185,129),'seething_reagent':(116,185,232),'enriching_reagent':(133,183,65),
    'soul_stripping_reagent':(88,198,202),'explosive_slurry':(185,138,85),'resonant_solution':(166,130,203),
    'sculk_suspension':(34,107,112),'purifying_solution':(200,236,220)}
jar=next((ROOT/'build/moddev/artifacts').glob('*minecraft-resources.jar'))
with ZipFile(jar) as z:
    empty=Image.open(BytesIO(z.read('assets/minecraft/textures/item/bucket.png'))).convert('RGBA')
    water=Image.open(BytesIO(z.read('assets/minecraft/textures/item/water_bucket.png'))).convert('RGBA')
for name,color in colors.items():
    icon=empty.copy()
    for y in range(16):
        for x in range(16):
            pixel=water.getpixel((x,y))
            if pixel!=empty.getpixel((x,y)):
                brightness=max(pixel[:3])/255
                icon.putpixel((x,y),tuple(round(c*(.7+.3*brightness)) for c in color)+(pixel[3],))
    icon.save(A/f'textures/item/{name}_bucket.png')
    (A/f'models/item/{name}_bucket.json').write_text(json.dumps({'parent':'minecraft:item/generated',
        'textures':{'layer0':f'ccd:item/{name}_bucket'}},indent=2)+'\n')
# Generic copper bucket has one stable shell icon; tooltip and bar show actual contents.
(A/'models/item/copper_bucket.json').write_text(json.dumps({'parent':'minecraft:item/generated',
    'textures':{'layer0':'ccd:item/copper_bucket'}},indent=2)+'\n')
