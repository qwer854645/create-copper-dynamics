"""Add native outer dial glass and use quiet cropped copper on faucet rim edges."""
import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
A=ROOT/'src/main/resources/assets/ccd'
def save(p,d): p.write_text(json.dumps(d,indent=2)+'\n',encoding='utf-8')
for state in ('closed','open'):
    path=A/f'models/block/redstone_fluid_valve/dial_{state}.json'
    model=json.loads(path.read_text())
    model['textures']['glass']='create:block/fluid_valve'
    model['elements']=[e for e in model['elements'] if not e.get('name','').startswith('outer_glass')]
    for side,y in (('up',14.12),('down',1.88)):
        model['elements'].append({'name':'outer_glass_'+side,'from':[2,y,2],'to':[14,y,14],
            'faces':{side:{'texture':'#glass','uv':[1,1,7,7]}}})
    save(path,model)
path=A/'models/item/redstone_fluid_valve.json'
model=json.loads(path.read_text())
model['textures']['glass']='create:block/fluid_valve'
# Inventory dial faces run along the same Y planes as the renderer partials.
for side,y in (('up',14.12),('down',1.88)):
    model['elements']=[e for e in model['elements'] if e.get('name')!='outer_glass_'+side]
    model['elements'].append({'name':'outer_glass_'+side,'from':[2,y,2],'to':[14,y,14],
        'faces':{side:{'texture':'#glass','uv':[1,1,7,7]}}})
save(path,model)
for state in ('closed','open'):
    path=A/f'models/block/faucet_{state}.json'
    model=json.loads(path.read_text())
    for element in model['elements']:
        if element.get('name','').startswith(('native_rim_connector','native_rim_north')):
            for side,face in element['faces'].items():
                if side in ('east','west','up','down'):
                    face['texture']='#base'
                    # A uniform native texel avoids mapping a whole directional
                    # stripe into the two-pixel rim thickness.
                    face['uv']=[8,8,9,9]
                    face.pop('rotation',None)
    save(path,model)
