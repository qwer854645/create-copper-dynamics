"""Keep native hatch geometry; adapt UVs to directly referenced 16px materials."""
import json
import copy
import io
from pathlib import Path
from zipfile import ZipFile
from PIL import Image

root=Path(__file__).resolve().parents[1]
jar=next((Path.home()/'.gradle/caches/modules-2/files-2.1/com.simibubi.create/create-1.21.1/6.0.10-280').rglob('*-slim.jar'))
with ZipFile(jar) as archive:
    geometry=json.loads(archive.read('assets/create/models/block/item_hatch/block_closed.json'))['elements']
    original=copy.deepcopy(geometry)
    mask=Image.open(io.BytesIO(archive.read('assets/create/textures/block/hatch.png'))).convert('RGBA')
    materials={name:Image.open(io.BytesIO(archive.read('assets/create/textures/block/'+name+'.png'))).convert('RGBA')
               for name in ('fluid_tank','brass_casing')}
for element in geometry:
    for face in element['faces'].values():
        face['uv']=[0,0,16,16]
        face.pop('rotation',None)
for name,body,lid in [('fluid_hatch','fluid_tank','copper_underside'),('smart_fluid_hatch','brass_casing','brass_block')]:
    elements=copy.deepcopy(geometry)
    # Restore the native triangular transparency and its precise rotated UVs on
    # the inclined part's two side faces only. All other material references stay.
    clipped=Image.new('RGBA',mask.size)
    for y in range(mask.height):
        for x in range(mask.width):
            color=materials[body].getpixel((x%16,y%16))
            clipped.putpixel((x,y),color[:3]+(mask.getpixel((x,y))[3],))
    destination=root/f'src/main/resources/assets/ccd/textures/block/{name}/side_clipped.png'
    clipped.save(destination)
    for side in ('east','west'):
        elements[2]['faces'][side]=copy.deepcopy(original[2]['faces'][side])
        elements[2]['faces'][side]['texture']='#side'
    model={'parent':'create:block/item_hatch/block_closed','textures':{
        '0':'create:block/'+body,'1':'create:block/'+lid,'side':f'ccd:block/{name}/side_clipped',
        'particle':'create:block/'+body},'render_type':'minecraft:cutout_mipped','elements':elements}
    (root/f'src/main/resources/assets/ccd/models/block/{name}_closed.json').write_text(json.dumps(model,indent=2)+'\n',encoding='utf-8')
