"""Normalize the two painted red rails to one redstone-controlled state."""
import json
from pathlib import Path
from PIL import Image
root=Path(__file__).resolve().parents[1]
assets=root/'src/main/resources/assets/ccd'
folder=assets/'textures/block/fluid_transmitter'
source=Image.open(folder/'side.png').convert('RGBA')
# Existing red columns: left x=1..2 is unpowered, right x=12..13 powered.
for powered in (False,True):
    image=source.copy()
    for y in range(2,7):
        for offset in (0,1):
            color=source.getpixel(((12 if powered else 2)+offset,y))
            for start in (2,12): image.putpixel((start+offset,y),color)
    image.save(folder/('side_powered.png' if powered else 'side_unpowered.png'))
path=assets/'models/block/fluid_transmitter.json'
model=json.loads(path.read_text(encoding='utf-8'))
model['textures']['side']='ccd:block/fluid_transmitter/side_unpowered'
path.write_text(json.dumps(model,indent=2)+'\n',encoding='utf-8')
(assets/'models/block/fluid_transmitter_powered.json').write_text(json.dumps({
    'parent':'ccd:block/fluid_transmitter','textures':{'side':'ccd:block/fluid_transmitter/side_powered'}
},indent=2)+'\n',encoding='utf-8')
path=assets/'blockstates/fluid_transmitter.json'
old=json.loads(path.read_text(encoding='utf-8'))['variants']
variants={}
for key,spec in old.items():
    if ',powered=true' in key: continue
    key=key.split(',powered=')[0]
    for powered in (False,True):
        variants[key+',powered='+str(powered).lower()]={**spec,'model':'ccd:block/fluid_transmitter'+('_powered' if powered else '')}
path.write_text(json.dumps({'variants':variants},indent=2)+'\n',encoding='utf-8')
