"""Open receiving troughs. Native materials, pipe rim and smart-pipe bezel UVs."""
import copy
import json
import sys
from pathlib import Path
from PIL import Image

sys.path.insert(0,str(Path.home()/'.config/opencode/skills/pixel-art/scripts'))
from pxl import Canvas

ROOT=Path(__file__).resolve().parents[1]
REF=Path.home()/'AppData/Local/Temp/opencode/create-art-reference/assets/create/models/block'
DEST=ROOT/'src/main/resources/assets/ccd/models/block'

def box(name,lo,hi,texture):
    x,y,z=lo; X,Y,Z=hi
    uv={'north':[16-X,16-Y,16-x,16-y],'south':[x,16-Y,X,16-y],
        'east':[16-Z,16-Y,16-z,16-y],'west':[z,16-Y,Z,16-y],
        'up':[x,z,X,Z],'down':[x,z,X,Z]}
    return {'name':name,'from':lo,'to':hi,'faces':{f:{'texture':'#'+texture,'uv':v} for f,v in uv.items()}}

for smart in (False,True):
    name='smart_fluid_hatch' if smart else 'fluid_hatch'
    elements=[]
    # Open mounting ring, leaving room for the native rear pipe mouth.
    for label,lo,hi in [('left',[1,0,1],[3,16,3]),('right',[13,0,1],[15,16,3]),
                        ('bottom',[3,0,1],[13,3,3]),('top',[3,13,1],[13,16,3])]:
        elements.append(box('mount_'+label,lo,hi,'rim'))
    # Two thin planar sides. Transparent pixels clip the triangular silhouette;
    # only the native copper/brass panel is sampled, without inventing new art.
    material='brass_block' if smart else 'copper_underside'
    source=Image.open(REF.parent.parent/'textures/block'/f'{material}.png').convert('RGBA')
    clipped=Canvas(16,16)
    for v in range(16):
        for u in range(16):
            if u<=v:
                clipped.set(u,v,source.getpixel((u,v)))
    texture=ROOT/f'src/main/resources/assets/ccd/textures/block/{name}/side_triangle.png'
    clipped.save(texture)
    for x in (2,13):
        side=box('smooth_side_'+str(x),[x,1,3],[x+1,12,14],'side')
        side['faces']={
            'east':{'texture':'#side','uv':[16,0,0,16]},
            'west':{'texture':'#side','uv':[0,0,16,16]}}
        elements.append(side)
    floor=box('inclined_trough_floor',[3,.5,8.8],[13,12,9.3],'body')
    floor['rotation']={'angle':-45,'axis':'x','origin':[8,5.15,9.5]}
    elements.append(floor)
    # Hollow volume above the inclined floor remains entirely open.
    for part in ('rim_connector','rim'):
        model=json.loads((REF/f'fluid_pipe/{part}/north.json').read_text())
        for original in model['elements']:
            e=copy.deepcopy(original);e['name']='native_rear_'+part
            for face in e['faces'].values():
                face['texture']='#pipe';face.pop('cullface',None)
            elements.append(e)
    if smart:
        # Frame fitted to the existing HatchFilterSlot plane (normal +Y,+Z).
        # UVs from the smart pipe front/rear bezel, not a new painted slot.
        bezel=box('smart_pipe_filter_bezel',[4,1.15,9.32],[12,9.15,9.48],'filter')
        bezel['rotation']={'angle':-45,'axis':'x','origin':[8,5.15,9.5]}
        for side in ('south','north'):
            bezel['faces'][side]['uv']=[8,8,16,16] if side=='south' else [0,8,8,16]
        for side in ('east','west','up','down'):
            bezel['faces'][side]['uv']=[13,0,16,8]
        elements.append(bezel)
    data={'parent':'create:block/item_hatch/block_closed','render_type':'minecraft:cutout_mipped',
          'textures':{'body':'create:block/brass_block' if smart else 'create:block/copper_underside',
                      'rim':'create:block/brass_casing' if smart else 'create:block/fluid_tank',
                      'pipe':'create:block/pipes','filter':'create:block/smart_pipe_2',
                      'side':f'ccd:block/{name}/side_triangle',
                      'particle':'#body'},'elements':elements}
    (DEST/(name+'_closed.json')).write_text(json.dumps(data,indent=2)+'\n',encoding='utf-8')
