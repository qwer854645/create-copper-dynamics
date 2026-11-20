"""Apply copper-package window rules to brass shells without repainting their metal."""
import json
from pathlib import Path
from PIL import Image
root=Path(__file__).resolve().parents[1]/'src/main/resources/assets/ccd'
texture=root/'textures/item/native_package_brass.png'
brass=Image.open(texture).convert('RGBA')
copper=Image.open(root/'textures/item/native_package_copper.png').convert('RGBA')
assert brass.size==copper.size
for y in range(brass.height):
    for x in range(brass.width):
        r,g,b,a=brass.getpixel((x,y))
        brass.putpixel((x,y),(r,g,b,min(a,copper.getpixel((x,y))[3])))
brass.save(texture)
for name in ('composite_package','brass_package_10x8','brass_package_12x10','brass_package_10x12'):
    path=root/f'models/item/{name}.json';model=json.loads(path.read_text())
    model['render_type']='minecraft:cutout_mipped'
    model['textures'].update(inner='create:block/brass_block',liquid='minecraft:block/water_still')
    model['elements']=[e for e in model['elements'] if not e.get('name','').startswith(('inner_lining','fluid_sample'))]
    shell=model['elements'][0];lo,hi=shell['from'],shell['to']
    model['elements'].append({'name':'inner_lining','from':[v-.04 for v in hi],'to':[v+.04 for v in lo],
        'faces':{side:{'texture':'#inner','uv':[0,0,16,16]} for side in ('north','south','east','west','up','down')}})
    # Two fluid samples in the cavity; the actual contents remain in components.
    for index in range(2):
        low=lo[1]+.15+(hi[1]-lo[1]-.3)*index/2
        high=lo[1]+.15+(hi[1]-lo[1]-.3)*(index+1)/2
        model['elements'].append({'name':'fluid_sample_'+str(index),
            'from':[lo[0]+.15,low,lo[2]+.15],'to':[hi[0]-.15,high,hi[2]-.15],
            'faces':{side:{'texture':'#liquid','uv':[0,0,16,16],'tintindex':index} for side in ('north','south','east','west','up','down')}})
    path.write_text(json.dumps(model,indent=2)+'\n',encoding='utf-8')
