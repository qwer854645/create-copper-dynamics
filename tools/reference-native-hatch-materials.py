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
    pipe_parts=[json.loads(archive.read(f'assets/create/models/block/fluid_pipe/{part}/south.json'))
                for part in ('rim_connector','rim')]
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
            color=materials['fluid_tank'].getpixel((x%16,y%16))
            clipped.putpixel((x,y),color[:3]+(mask.getpixel((x,y))[3],))
    destination=root/f'src/main/resources/assets/ccd/textures/block/{name}/side_clipped.png'
    clipped.save(destination)
    for side in ('east','west'):
        elements[2]['faces'][side]=copy.deepcopy(original[2]['faces'][side])
        elements[2]['faces'][side]['texture']='#side'
    if name=='smart_fluid_hatch':
        elements[2]['faces']['down']['texture']='#front_copper'
    # Replace only the upward-facing inclined cover with an actual pipe mouth.
    # Surrounding strips bridge the native ten-pixel mouth to the old cover edges.
    elements[2]['faces'].pop('south',None)
    strips=[('left',[2,0,12.55],[3,10.6,12.6]),
                        ('right',[13,0,12.55],[14,10.6,12.6]),
                        ('bottom',[3,0,12.55],[13,.3,12.6]),
                         ('top',[3,10.3,12.55],[13,10.6,12.6])]
    if name=='smart_fluid_hatch':
        # Cut the opening into the original brass cover itself: four coplanar
        # strips retain continuous UVs, with no copper pipe fitting on this model.
        strips=[('left',[2,0,12.55],[4,10.6,12.6]),
                ('right',[12,0,12.55],[14,10.6,12.6]),
                ('bottom',[4,0,12.55],[12,1.5,12.6]),
                ('top',[4,9.1,12.55],[12,10.6,12.6])]
    for label,lo,hi in strips:
        elements.append({'name':'mouth_surround_'+label,'from':lo,'to':hi,
            'rotation':copy.deepcopy(original[2]['rotation']),
            'faces':{'south':{'texture':'#1','uv':[lo[0],16-hi[1],hi[0],16-lo[1]]}}})
    for part_index,part in enumerate(pipe_parts if name=='fluid_hatch' else []):
        for native_part in part['elements']:
            mouth=copy.deepcopy(native_part)
            mouth['name']='native_inclined_pipe_mouth_'+str(part_index)
            for corner in ('from','to'):
                mouth[corner]=[mouth[corner][0],mouth[corner][1]-2.7,mouth[corner][2]-3.4]
            mouth['rotation']=copy.deepcopy(original[2]['rotation'])
            for face in mouth['faces'].values():
                face['texture']='#pipe'
                face.pop('cullface',None)
            elements.append(mouth)
    if name=='smart_fluid_hatch':
        # NORTH rotates the native offset to (8,5.15,6.5), not z=9.5.
        # Project onto the hatch underside z-y=2, then offset only .01px
        # outwards. Native ValueBox scale is .5*.965 -> 7.72 model pixels.
        elements.append({
            'name':'smart_pipe_filter_slot',
            'from':[4.14,.965,6.795],'to':[11.86,8.685,6.835],
            'rotation':{'angle':45,'axis':'x','origin':[8,4.825,6.825]},
            'faces':{
                'south':{'texture':'#filter','uv':[8,8,16,16]},
                'north':{'texture':'#filter','uv':[0,8,8,16]},
                'east':{'texture':'#filter','uv':[13,0,16,8]},
                'west':{'texture':'#filter','uv':[13,8,16,0]},
                'up':{'texture':'#filter','uv':[13,8,16,0],'rotation':270},
                'down':{'texture':'#filter','uv':[13,0,16,8],'rotation':90}
            }})
    model={'parent':'create:block/item_hatch/block_closed','textures':{
        '0':'create:block/'+body,'1':'create:block/copper_underside','side':f'ccd:block/{name}/side_clipped',
        'front_copper':'create:block/fluid_tank',
        'filter':'create:block/smart_pipe_2',
        'pipe':'create:block/pipes',
        'particle':'create:block/'+body},'render_type':'minecraft:cutout_mipped','elements':elements}
    (root/f'src/main/resources/assets/ccd/models/block/{name}_closed.json').write_text(json.dumps(model,indent=2)+'\n',encoding='utf-8')
