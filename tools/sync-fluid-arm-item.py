"""Use the current live syringe partial in the inventory model's folded pose."""
import copy,json
from pathlib import Path
root=Path(__file__).resolve().parents[1]/'src/main/resources/assets/ccd/models'
path=root/'item/fluid_arm.json'
item=json.loads(path.read_text(encoding='utf-8'))
head=json.loads((root/'block/fluid_arm/claw_base.json').read_text(encoding='utf-8'))
start=next(i for i,e in enumerate(item['elements']) if e.get('name')=='Joint 4')
item['elements']=item['elements'][:start]
item['textures'].update(head['textures'])
item['render_type']='minecraft:cutout'
for element in head['elements']:
    e=copy.deepcopy(element)
    for corner in ('from','to'):
        e[corner]=[e[corner][0]+8,e[corner][1]+9,e[corner][2]+4]
    if 'rotation' in e:
        p=e['rotation']['origin'];e['rotation']['origin']=[p[0]+8,p[1]+9,p[2]+4]
    item['elements'].append(e)
path.write_text(json.dumps(item,indent=2)+'\n',encoding='utf-8')
assert len(item['elements'])==start+len(head['elements'])
print('Item head synchronized with live partial:',len(head['elements']),'elements')
