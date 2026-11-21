"""Import approved artwork unchanged; synchronize inventory bodies and runtime dial parts."""
import copy
import json
import sys
from pathlib import Path
from zipfile import ZipFile

root=Path(__file__).resolve().parents[1]
assets=root/'src/main/resources/assets/ccd'
archive=Path(sys.argv[1])
def read(relative):return json.loads((assets/relative).read_text(encoding='utf-8'))
def save(relative,data):
    (assets/relative).write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
with ZipFile(archive) as z:
    for name in z.namelist():
        prefix='aaa/assets/ccd/'
        if not name.startswith(prefix) or name.endswith('/'):continue
        relative=Path(name[len(prefix):])
        if '..' in relative.parts:raise ValueError(name)
        path=assets/relative
        backup=root/'outputs/art-revision-7-backup'/relative
        if path.exists() and not backup.exists():
            backup.parent.mkdir(parents=True,exist_ok=True);backup.write_bytes(path.read_bytes())
        path.parent.mkdir(parents=True,exist_ok=True);path.write_bytes(z.read(name))

# Block artwork is authoritative for the inventory shell. Retain item-only parts and displays.
for item,body_id,parts in [
    ('flow_meter','flow_meter/body',6),
    ('vertical_turning_booster_pump','vertical_turning_booster_pump',9)
]:
    body=read('models/block/'+body_id+'.json');inventory=read('models/item/'+item+'.json')
    inventory['elements'][:parts]=copy.deepcopy(body['elements'])
    inventory['textures'].update(body.get('textures',{}))
    save('models/item/'+item+'.json',inventory)

# The revision supplies the redstone dial only in item form: copy it to its runtime parts.
inventory=read('models/item/redstone_fluid_pump.json')
for part,names in [('block',None),('pointer',{'flow_pointer'})]:
    path='models/block/redstone_fluid_pump/'+part+'.json'
    current=read(path)
    current['textures']=copy.deepcopy(inventory['textures'])
    current['elements']=[copy.deepcopy(e) for e in inventory['elements']
                         if (e.get('name') in names if names else e.get('name')!='flow_pointer' and not e.get('name','').startswith('inventory_shaft_'))]
    save(path,current)
print('Imported revision 7; item shells and redstone runtime dial synchronized.')
