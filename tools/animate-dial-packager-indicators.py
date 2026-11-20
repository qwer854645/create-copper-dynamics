"""Separate animated needles and bind existing packager status artwork to power."""
import json,copy
from pathlib import Path
root=Path(__file__).resolve().parents[1]/'src/main/resources/assets/ccd'
def save(p,d):p.write_text(json.dumps(d,indent=2)+'\n',encoding='utf-8')
for state in ('powered','unpowered'):
    path=root/f'models/block/three_way_valve/dial_{state}.json';model=json.loads(path.read_text())
    pointer=next((e for e in model['elements'] if e['name']=='pointer'),None)
    if state=='unpowered' and pointer:
        save(root/'models/block/three_way_valve/pointer.json',{'render_type':'minecraft:cutout_mipped',
            'textures':{'needle':model['textures']['needle']},'elements':[pointer]})
    model['elements']=[e for e in model['elements'] if e['name']!='pointer'];save(path,model)
for name,key,texture in [('fluid_packager','2','horizontal'),('fluid_packager_vertical','5','vertical')]:
    path=root/f'models/block/{name}.json';model=json.loads(path.read_text())
    powered={'parent':'ccd:block/'+name,'textures':{key:f'ccd:block/fluid_packager/packager_{texture}_powered'}}
    save(root/f'models/block/{name}_powered.json',powered)
path=root/'blockstates/fluid_packager.json';old=json.loads(path.read_text())['variants'];states={}
for key,spec in old.items():
    if ',powered=' in key: continue
    for power in (False,True):
        states[key+',powered='+str(power).lower()]={**spec,'model':spec['model']+('_powered' if power else '')}
save(path,{'variants':states})
