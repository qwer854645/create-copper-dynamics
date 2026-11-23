"""Import user geometry without losing 1.21.1 state variants or arbitrary gear angles."""
from pathlib import Path
import json,copy,shutil,importlib.util
root=Path(__file__).resolve().parents[1]
assets=root/'src/main/resources/assets/ccd'
source=Path(r'C:\Users\Administrator\Downloads')
out=root/'outputs/variable-pump-user-import';out.mkdir(parents=True,exist_ok=True)
body=json.loads((source/'body.json').read_text(encoding='utf-8-sig'))
item=json.loads((source/'variable_flow_pump.json').read_text(encoding='utf-8-sig'))
gear=item['elements'][14:]
assert len(gear)==8
def save(name,data):
 p=assets/name;b=out/'backup'/name
 if p.exists() and not b.exists():b.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(p,b)
 p.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def composite(elements):
 children={}
 for i,element in enumerate(elements):
  e=copy.deepcopy(element);rot=e.pop('rotation',None)
  child={'parent':'minecraft:block/block','textures':item['textures'],'elements':[e]}
  if rot and 'x' in rot:
   assert rot.get('y',0)==rot.get('z',0)==0
   assert rot['origin'][1:]==[8,8]
   child['transform']={'rotation':{'x':rot['x']},'origin':'center'}
  elif rot:e['rotation']=rot
  children[str(i)]=child
 return {'parent':'create:block/block','loader':'neoforge:composite','render_type':'minecraft:cutout_mipped',
         'textures':item['textures'],'children':children,'item_render_order':list(children)}
save('models/block/variable_flow_pump/body.json',body)
save('models/item/variable_flow_pump.json',composite(item['elements']))
save('models/block/variable_flow_pump/cog.json',composite(gear))
# Render actual imported geometry with equivalent rotations for the offline preview.
preview=copy.deepcopy(item)
for e in preview['elements']:
 r=e.get('rotation',{})
 if 'x' in r:e['rotation']={'axis':'x','angle':r['x'],'origin':r['origin']}
spec=importlib.util.spec_from_file_location('preview',root/'tools/preview-smart-grate-indicators.py')
p=importlib.util.module_from_spec(spec);spec.loader.exec_module(p)
lookup=p.model;p.model=lambda name:preview if name=='imported' else lookup(name)
p.render('imported').save(out/'preview.png')
print('Imported body/item and synchronized dynamic cog; all blockstate and roll wrappers retained.')
