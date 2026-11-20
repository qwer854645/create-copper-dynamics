"""Bind valve lamps to POWERED and keep inventory geometry in sync with visible partials."""
import json,copy
from pathlib import Path
r=Path(__file__).resolve().parents[1]/'src/main/resources/assets/ccd'
def load(p):return json.loads((r/p).read_text(encoding='utf-8'))
def save(p,m):(r/p).write_text(json.dumps(m,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
# ENABLED controls passage/dial; POWERED independently controls the lamp artwork.
p='blockstates/redstone_fluid_valve.json';states=load(p);variants={}
for key,spec in states['variants'].items():
 if ',powered=' in key:key=key.split(',powered=')[0]
 for powered in (False,True):
  model=spec['model'].split('ccd:')[1]
  for suffix in ('_unpowered','_powered'):
   if model.endswith(suffix):model=model[:-len(suffix)]
  source=load('models/'+model+'.json')
  name=model+('_powered' if powered else '_unpowered')
  textures={k:('ccd:block/block_horizontal/lamp_on' if powered else 'ccd:block/block_horizontal/lamp_off')
   for k,v in source['textures'].items() if '/lamp_' in v}
  save('models/'+name+'.json',{'parent':'ccd:'+model,'textures':textures})
  variants[key+',powered='+str(powered).lower()]={**spec,'model':'ccd:'+name}
states['variants']=variants;save(p,states)
for state in ('open','closed'):
 p=f'models/block/redstone_fluid_valve/dial_{state}.json';m=load(p)
 for e in m['elements']:
  if e.get('name')=='outer_dial_front':e['from'][1]=e['to'][1]=14.01
  if e.get('name')=='outer_dial_back':e['from'][1]=e['to'][1]=1.99
 save(p,m)
# Rebuild pump item from precisely the block body and its runtime pointer.
p='models/item/redstone_fluid_pump.json';old=load(p);body=load('models/block/redstone_fluid_pump/block.json')
pointer=load('models/block/redstone_fluid_pump/pointer.json')
shafts=[e for e in old['elements'] if e.get('name','').startswith('inventory_shaft_')]
save(p,{**copy.deepcopy(body),'parent':'create:block/block','textures':{**body['textures'],**pointer['textures'],
 'shaft_side':'create:block/axis','shaft_end':'create:block/axis_top'},'elements':body['elements']+pointer['elements']+shafts})
