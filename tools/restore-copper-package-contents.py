"""Add inward-facing copper lining and tintable liquid geometry to all copper sizes."""
import json,copy
from pathlib import Path
root=Path(__file__).resolve().parents[1]/'src/main/resources/assets/ccd/models/item'
for name in ('fluid_package','copper_package_10x8','copper_package_12x10','copper_package_10x12'):
    path=root/(name+'.json');model=json.loads(path.read_text(encoding='utf-8'))
    model['render_type']='minecraft:cutout_mipped'
    model['textures'].update(inner='create:block/copper_underside',liquid='minecraft:block/water_still')
    model['elements']=[e for e in model['elements'] if e.get('name') not in ('inner_lining','fluid_volume')]
    shell=model['elements'][0];lo=shell['from'];hi=shell['to']
    lining={'name':'inner_lining','from':[v-.04 for v in hi],'to':[v+.04 for v in lo],
        'faces':{side:{'texture':'#inner','uv':[0,0,16,16]} for side in ('north','south','east','west','up','down')}}
    liquid={'name':'fluid_volume','from':[lo[0]+.15,lo[1]+.15,lo[2]+.15],
        'to':[hi[0]-.15,hi[1]-.15,hi[2]-.15],
        'faces':{side:{'texture':'#liquid','uv':[0,0,16,16],'tintindex':0} for side in ('north','south','east','west','up','down')}}
    model['elements'] += [lining,liquid]
    path.write_text(json.dumps(model,indent=2)+'\n',encoding='utf-8')
