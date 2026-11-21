"""Import revision five, repairing unsupported geometry and synchronizing inventory parts."""
import copy,json
from pathlib import Path
from zipfile import ZipFile
root=Path(__file__).resolve().parents[1];assets=root/'src/main/resources/assets/ccd';out=root/'outputs/art-revision-5'
def read(rel):return json.loads((assets/rel).read_text(encoding='utf-8'))
def write(rel,data):
 p=assets/rel;b=out/'backup'/rel
 if p.exists() and not b.exists():b.parent.mkdir(parents=True,exist_ok=True);b.write_bytes(p.read_bytes())
 p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(data)
def save(rel,data):write(rel,(json.dumps(data,ensure_ascii=False,indent=2)+'\n').encode())
old_turn=read('models/item/turning_booster_pump.json')
with ZipFile(Path.home()/'Downloads/aaa (5).zip') as z:
 for n in z.namelist():
  if '/assets/ccd/' not in n or n.endswith('/'):continue
  rel=n.split('/assets/ccd/')[1]
  # Unused legacy cross dial has non-Minecraft Euler rotation. Replace by inheritance below.
  if rel=='models/block/three_way_valve/dial_unpowered_cross.json':continue
  write(rel,z.read(n))
save('models/block/three_way_valve/dial_unpowered_cross.json',{'parent':'ccd:block/three_way_valve/dial_unpowered'})
for state in ('powered','unpowered'):
 rel=f'models/block/three_way_valve/dial_{state}.json';m=read(rel)
 for e in m['elements']:
  if e.get('name')=='dial':e['from'][1],e['to'][1]=13.02,14.02
  e.pop('rotation',None)
 save(rel,m)
rel='models/block/three_way_valve/pointer.json';m=read(rel)
for e in m['elements']:e['from'][1]=e['to'][1]=14.07
save(rel,m)
# Keep redstone pump's two approved green stops, but accept the recessed dial and pointer.
rel='models/block/redstone_fluid_pump/block.json';m=read(rel)
m['textures']['dial']='ccd:block/redstone_fluid_pump/dial'
m['elements']=[e for e in m['elements'] if e.get('name')!='red_side']
for e in m['elements']:
 if e.get('name')=='dial':e['from'][1],e['to'][1]=13.01,13.6;e['faces']['up']['rotation']=0
 if e.get('name','').startswith('unused_stop_background_'):e['from'][1]=e['to'][1]=13.62
 e.pop('rotation',None)
m['elements'].insert(0,{'name':'solid_dial_backing','from':[2,13,2],'to':[14,13.01,14],
 'faces':{s:{'texture':'#housing','uv':[2,2,14,14]} for s in ('north','south','east','west','up','down')}})
save(rel,m)
rel='models/item/redstone_fluid_pump.json';item=read(rel);pointer=read('models/block/redstone_fluid_pump/pointer.json')
shafts=[e for e in item['elements'] if e.get('name','').startswith('inventory_shaft_')]
save(rel,{**copy.deepcopy(m),'parent':'create:block/block','textures':{**m['textures'],**pointer['textures'],'shaft_side':'create:block/axis','shaft_end':'create:block/axis_top'},'elements':m['elements']+pointer['elements']+shafts})
# Imported valve dial cannot sit inside its opaque housing.
for state in ('open','closed'):
 rel=f'models/block/redstone_fluid_valve/dial_{state}.json';m=read(rel)
 for e in m['elements']:
  if e.get('name')=='outer_dial_front':e['from'][1]=e['to'][1]=14.01
  if e.get('name')=='outer_dial_back':e['from'][1]=e['to'][1]=1.99
 save(rel,m)
# Remove the extra full cube only; it overlaps the tailored reversed cross housing.
rel='models/block/turning_booster_pump_reversed_cross.json';m=read(rel)
m['elements']=[e for e in m['elements'] if not(e.get('name')=='copper_housing' and e['from']==[2,2,2] and e['to']==[14,14,14])]
save(rel,m)
body=read('models/block/turning_booster_pump.json')
shafts=[e for e in old_turn['elements'] if e.get('name','').startswith('inventory_shaft_')]
save('models/item/turning_booster_pump.json',{**copy.deepcopy(body),'textures':{**body['textures'],**{k:v for k,v in old_turn['textures'].items() if k.startswith('shaft_')}},'elements':body['elements']+shafts})
print('Revision five imported; original PNG bytes preserved; invalid cross rotation and overlapping cube removed.')
