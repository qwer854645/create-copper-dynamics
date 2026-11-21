"""Portable pump shares existing bellows geometry without copying models or textures."""
import json
from pathlib import Path
root=Path(__file__).resolve().parents[1]/'src/main/resources'
def save(rel,data):
 p=root/rel;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
save('assets/ccd/models/item/portable_fluid_pump.json',{'parent':'ccd:item/reciprocating_fluid_pump'})
save('assets/ccd/blockstates/portable_fluid_pump.json',json.loads((root/'assets/ccd/blockstates/reciprocating_fluid_pump.json').read_text()))
save('data/ccd/recipe/portable_fluid_pump.json',{'type':'minecraft:crafting_shapeless','ingredients':[{'item':'create:mechanical_pump'},{'item':'create:hand_crank'},{'item':'create:belt_connector'}],'result':{'id':'ccd:portable_fluid_pump','count':1}})
save('data/ccd/loot_table/blocks/portable_fluid_pump.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'ccd:portable_fluid_pump'}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
for rel in ('data/create/tags/block/wrench_pickup.json','data/minecraft/tags/block/mineable/pickaxe.json'):
 data=json.loads((root/rel).read_text());
 if 'ccd:portable_fluid_pump' not in data['values']:data['values'].append('ccd:portable_fluid_pump')
 save(rel,data)
for locale in ('zh_cn','en_us'):
 rel=f'assets/ccd/lang/{locale}.json';data=json.loads((root/rel).read_text(encoding='utf-8'))
 data['block.ccd.portable_fluid_pump']='便携式流体泵' if locale=='zh_cn' else 'Portable Fluid Pump'
 data['block.ccd.portable_fluid_pump.tooltip.summary']='用于_短距离快速转移_流体，可接入旋转动力或像_手摇曲柄_一样右键驱动。潜行右键反向。' if locale=='zh_cn' else 'Transfers fluids _quickly over short distances_. Supply rotation or _R-Click like a Hand Crank_ to pump manually. Sneak to reverse.'
 save(rel,data)
