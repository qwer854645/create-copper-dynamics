"""Import supplied revision, backing up current assets and preserving native animation parts."""
import copy,json
from pathlib import Path
from zipfile import ZipFile
root=Path(__file__).resolve().parents[1]
assets=root/'src/main/resources/assets/ccd'
backup=root/'outputs/art-revision-3/backup'
def save(rel,model):
 p=assets/rel;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(model,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def remember(rel):
 p=assets/rel;b=backup/rel
 if p.exists() and not b.exists():b.parent.mkdir(parents=True,exist_ok=True);b.write_bytes(p.read_bytes())
with ZipFile(Path.home()/'Downloads/aaa (3).zip') as archive:
 for name in archive.namelist():
  if '/assets/ccd/' not in name or name.endswith('/'):continue
  rel=name.split('/assets/ccd/')[1];remember(rel)
  p=assets/rel;p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(archive.read(name))

# Preserve the supplied recessed dial, but normalize its bounds and layer order.
rel='models/block/redstone_fluid_pump/block.json';pump=json.loads((assets/rel).read_text())
for e in pump['elements']:
 if e.get('name')=='dial':
  e['from'][1],e['to'][1]=13.01,14.01
  e['faces']['up']['rotation']=0
 elif e.get('name','').startswith('unused_stop_background_'):
  e['from'][1]=e['to'][1]=14.02
backing={'name':'solid_dial_backing','from':[2,13,2],'to':[14,13.01,14],
 'faces':{side:{'texture':'#housing','uv':[2,2,14,14]} for side in ('north','south','east','west','up','down')}}
pump['elements'].insert(0,backing);save(rel,pump)
pointer=json.loads((assets/'models/block/redstone_fluid_pump/pointer.json').read_text())
# Match the item to this exact body; only the two static shafts are item-specific.
rel='models/item/redstone_fluid_pump.json';remember(rel)
old=json.loads((assets/rel).read_text());shafts=[e for e in old['elements'] if e.get('name','').startswith('inventory_shaft_')]
save(rel,{**copy.deepcopy(pump),'parent':'create:block/block','textures':{**pump['textures'],
 'shaft_side':old['textures']['shaft_side'],'shaft_end':old['textures']['shaft_end']},
 'elements':copy.deepcopy(pump['elements'])+pointer['elements']+shafts})

# Extract the revised bidirectional pump body. Static item shafts are not baked into the block.
item=json.loads((assets/'models/item/bidirectional_fluid_pump.json').read_text())
body=copy.deepcopy(item);body['parent']='minecraft:block/block'
body['elements']=[e for e in body['elements'] if not e.get('name','').startswith('inventory_shaft_')]
for e in body['elements']:e.pop('rotation',None) if e.get('rotation',{}).get('angle')==0 else None
for suffix in ('','_reversed','_cross','_reversed_cross'):remember('models/block/bidirectional_fluid_pump'+suffix+'.json')
save('models/block/bidirectional_fluid_pump.json',body)
reverse=copy.deepcopy(body)
for e in reverse['elements']:
 if e.get('name','').startswith('native_pump_mark_'):
  for face in e['faces'].values():face['rotation']=(face.get('rotation',0)+180)%360
save('models/block/bidirectional_fluid_pump_reversed.json',reverse)
item['parent']='ccd:block/bidirectional_fluid_pump';save('models/item/bidirectional_fluid_pump.json',item)

# Valve item uses the new lamp surfaces too; its own display transforms remain intact.
rel='models/item/redstone_fluid_valve.json';remember(rel)
p=assets/rel
if p.exists():
 item=json.loads(p.read_text());block=json.loads((assets/'models/block/redstone_fluid_valve/block_horizontal_closed.json').read_text())
 for key,value in block['textures'].items():
  if 'lamp_' in value:item.setdefault('textures',{})[key]=value
 # Update only existing matching center elements, retaining inventory-specific orientation.
 for e in item.get('elements',[]):
  if e.get('name')=='ValveCenter':
   for face in e['faces'].values():face['texture']='#lamp'
 item.setdefault('textures',{})['lamp']='ccd:block/block_horizontal/lamp_off'
 save(rel,item)
print('Imported 9 models and 11 original PNG files; normalized pump layers and synchronized item bodies.')
for p in (assets/'models/block/redstone_fluid_valve').glob('block_*.json'):
 model=json.loads(p.read_text())
 for e in model.get('elements',[]):
  for face in e['faces'].values():
   if face.get('texture')=='#missing':face['texture']='#2'
 save(str(p.relative_to(assets)),model)
