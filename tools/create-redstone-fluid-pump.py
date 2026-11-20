"""Valve-shaped, single-dial pump with native shaft holes and pipe rims."""
import json,copy
from pathlib import Path
from zipfile import ZipFile
root=Path(__file__).resolve().parents[1]/'src/main/resources'
def save(rel,data):
 p=root/rel;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
jar=next((Path.home()/'.gradle/caches/modules-2/files-2.1/com.simibubi.create/create-1.21.1/6.0.10-280').rglob('*-slim.jar'))
faces={side:{'texture':'#valve','uv':[1,1,7,7]} for side in ('north','south','east','west','up','down')}
for side in ('east','west'):faces[side]={'texture':'#valve','uv':[9,1,15,7]}
elements=[{'name':'valve_housing','from':[2,2,2],'to':[14,14,14],'faces':faces}]
with ZipFile(jar) as archive:
 for side in ('north','south'):
  for part in ('rim_connector','rim'):
   model=json.loads(archive.read(f'assets/create/models/block/fluid_pipe/{part}/{side}.json'))
   for e in model['elements']:
    for f in e['faces'].values():f['texture']='#pipe';f.pop('cullface',None)
    elements.append(e)
# One dial only. Its neutral scale is native artwork; needle points along the pipe axis.
elements.append({'name':'dial','from':[2,14.01,2],'to':[14,14.01,14],
 'faces':{'up':{'texture':'#dial','uv':[2,2,14,14]}}})
elements.append({'name':'dial_glass','from':[2,14.14,2],'to':[14,14.14,14],
 'faces':{'up':{'texture':'#valve','uv':[1,1,7,7]}}})
pointer={'name':'flow_pointer','from':[7.5,14.08,4.5],'to':[8.5,14.08,11.5],
 'faces':{'up':{'texture':'#valve','uv':[2,7.5,5.5,8],'rotation':270}}}
textures={'valve':'create:block/fluid_valve','pipe':'create:block/pipes','dial':'create:block/valve_open','particle':'create:block/copper_underside'}
model={'parent':'minecraft:block/block','render_type':'minecraft:cutout_mipped','ambientocclusion':False,'textures':textures,'elements':elements}
save('assets/ccd/models/block/redstone_fluid_pump/block.json',model)
save('assets/ccd/models/block/redstone_fluid_pump/pointer.json',{'render_type':'minecraft:cutout_mipped','textures':textures,'elements':[pointer]})
save('assets/ccd/models/item/redstone_fluid_pump.json',{**copy.deepcopy(model),'parent':'create:item/package_transforms','elements':copy.deepcopy(elements)+[pointer]})
variants={}
for facing,rotation in {'north':{},'south':{'y':180},'east':{'y':90},'west':{'y':270},'up':{'x':90},'down':{'x':270}}.items():
 variants['facing='+facing]={'model':'ccd:block/redstone_fluid_pump/block',**rotation}
save('assets/ccd/blockstates/redstone_fluid_pump.json',{'variants':variants})
save('data/ccd/recipe/redstone_fluid_pump.json',{'type':'minecraft:crafting_shapeless','category':'redstone','ingredients':[
 {'item':'create:mechanical_pump'},{'item':'ccd:redstone_fluid_valve'},{'item':'create:shaft'}],'result':{'id':'ccd:redstone_fluid_pump','count':1}})
save('data/ccd/loot_table/blocks/redstone_fluid_pump.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'ccd:redstone_fluid_pump'}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
for tag in ('data/create/tags/block/wrench_pickup.json','data/minecraft/tags/block/mineable/pickaxe.json'):
 p=root/tag;data=json.loads(p.read_text());
 if 'ccd:redstone_fluid_pump' not in data['values']:data['values'].append('ccd:redstone_fluid_pump')
 save(tag,data)
for locale in ('zh_cn','en_us'):
 p=root/f'assets/ccd/lang/{locale}.json';data=json.loads(p.read_text(encoding='utf-8'))
 data['block.ccd.redstone_fluid_pump']='红石流体泵' if locale=='zh_cn' else 'Redstone Fluid Pump'
 data['block.ccd.redstone_fluid_pump.tooltip.summary']='使用旋转动力泵送流体，_红石信号_反转流向。' if locale=='zh_cn' else 'Pumps fluids using rotational power. A _Redstone Signal_ reverses the flow.'
 save(f'assets/ccd/lang/{locale}.json',data)
