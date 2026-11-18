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
    if name=='smart_fluid_hatch':
        # The outward face sits just behind HatchFilterSlot's (8,5.15,9.5)
        # origin. NORTH hatch orientation turns the native filter transform's
        # local -45 degree tilt into +45 degrees on the model's south face.
        # Icon and hitbox stay unchanged.
        elements.append({
            'name':'smart_pipe_filter_slot',
            'from':[4,1.15,9.28],'to':[12,9.15,9.48],
            'rotation':{'angle':45,'axis':'x','origin':[8,5.15,9.5]},
            'faces':{
                'south':{'texture':'#filter','uv':[8,8,16,16]},
                'north':{'texture':'#filter','uv':[0,8,8,16]},
                'east':{'texture':'#filter','uv':[13,0,16,8]},
                'west':{'texture':'#filter','uv':[13,8,16,0]},
                'up':{'texture':'#filter','uv':[13,8,16,0],'rotation':270},
                'down':{'texture':'#filter','uv':[13,0,16,8],'rotation':90}
            }})
    model={'parent':'create:block/item_hatch/block_closed','textures':{
        '0':'create:block/'+body,'1':'create:block/'+lid,'side':f'ccd:block/{name}/side_clipped',
        'filter':'create:block/smart_pipe_2',
        'particle':'create:block/'+body},'render_type':'minecraft:cutout_mipped','elements':elements}
    (root/f'src/main/resources/assets/ccd/models/block/{name}_closed.json').write_text(json.dumps(model,indent=2)+'\n',encoding='utf-8')
