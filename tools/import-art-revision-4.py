"""Import user artwork without regenerating textures or touching the tall piston pump."""
import copy,json
from pathlib import Path
from zipfile import ZipFile
root=Path(__file__).resolve().parents[1]
assets=root/'src/main/resources/assets/ccd'
backup=root/'outputs/art-revision-4/backup'
def remember(rel):
 p=assets/rel;b=backup/rel
 if p.exists() and not b.exists():b.parent.mkdir(parents=True,exist_ok=True);b.write_bytes(p.read_bytes())
def write(rel,data):
 remember(rel);p=assets/rel;p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(data)
def save(rel,data):write(rel,(json.dumps(data,ensure_ascii=False,indent=2)+'\n').encode())
def read(rel):return json.loads((assets/rel).read_text(encoding='utf-8'))
with ZipFile(Path.home()/'Downloads/aaa (4).zip') as archive:
 for name in archive.namelist():
  if '/assets/ccd/' not in name or name.endswith('/'):continue
  rel=name.split('/assets/ccd/')[1]
  if rel.startswith('models/block/reciprocating_fluid_pump/'):
   part=Path(rel).name
   model=json.loads(archive.read(name))
   if part=='body.json':
    model['textures']['copper']='ccd:block/portable_fluid_pump/base'
    model['textures']['particle']='ccd:block/portable_fluid_pump/base'
   save('models/block/portable_fluid_pump/'+part,model)
  else:write(rel,archive.read(name))

# Portable body is a single source; partials live under its own namespace now.
body=read('models/block/portable_fluid_pump.json')
save('models/block/portable_fluid_pump/body.json',body)
save('models/block/portable_fluid_pump.json',{'parent':'ccd:block/portable_fluid_pump/body'})
old=read('models/item/portable_fluid_pump.json')
shafts=[e for e in old['elements'] if e.get('name') in ('Axis','inventory_shaft_90','inventory_shaft_270')]
assert len(shafts)==2
bellows=read('models/block/portable_fluid_pump/bellows.json');plate=read('models/block/portable_fluid_pump/plate.json')
save('models/item/portable_fluid_pump.json',{
 'parent':'create:block/block','render_type':'minecraft:cutout_mipped',
 'textures':{**body['textures'],**bellows['textures'],**plate['textures'],**{k:v for k,v in old['textures'].items() if 'shaft_' in k}},
 'elements':body['elements']+bellows['elements']+plate['elements']+shafts})

# Match the pump inventory body to the supplied forward block; retain its static shaft pair.
body=read('models/block/bidirectional_fluid_pump.json');old=read('models/item/bidirectional_fluid_pump.json')
shafts=[e for e in old['elements'] if e.get('name','').startswith('inventory_shaft_')]
save('models/item/bidirectional_fluid_pump.json',{
 **copy.deepcopy(body),'parent':'create:block/block',
 'textures':{**body['textures'],'shaft_side':'create:block/axis','shaft_end':'create:block/axis_top'},
 'elements':body['elements']+shafts})

# Preserve the approved green two-stop face and correct the imported inverted bounds.
body=read('models/block/redstone_fluid_pump/block.json')
body['textures']['dial']='ccd:block/redstone_fluid_pump/dial'
body['elements']=[e for e in body['elements'] if e.get('name')!='red_side']
for e in body['elements']:
 if e.get('name')=='dial':
  e['from'][1],e['to'][1]=13.01,14.01
  e['faces']['up']['rotation']=0
 if e.get('name','').startswith('unused_stop_background_'):e['from'][1]=e['to'][1]=14.02
body['elements'].insert(0,{'name':'solid_dial_backing','from':[2,13,2],'to':[14,13.01,14],
 'faces':{side:{'texture':'#housing','uv':[2,2,14,14]} for side in ('north','south','east','west','up','down')}})
save('models/block/redstone_fluid_pump/block.json',body)
old=read('models/item/redstone_fluid_pump.json');pointer=read('models/block/redstone_fluid_pump/pointer.json')
shafts=[e for e in old['elements'] if e.get('name','').startswith('inventory_shaft_')]
save('models/item/redstone_fluid_pump.json',{
 **copy.deepcopy(body),'parent':'create:block/block',
 'textures':{**body['textures'],**pointer['textures'],'shaft_side':'create:block/axis','shaft_end':'create:block/axis_top'},
 'elements':body['elements']+pointer['elements']+shafts})
print('Imported revision 4; portable partials separated, inventory bodies synchronized, green dial retained.')
