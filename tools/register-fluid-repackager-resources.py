"""Reference native repackager artwork; add dedicated fluid-aware device resources."""
import json
from pathlib import Path
from zipfile import ZipFile
root=Path(__file__).resolve().parents[1]
res=root/'src/main/resources'
def save(relative,value):
    path=res/relative;path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
jar=next((Path.home()/'.gradle/caches/modules-2/files-2.1/com.simibubi.create/create-1.21.1/6.0.10-280').rglob('*-slim.jar'))
with ZipFile(jar) as z:
    save('assets/ccd/blockstates/fluid_repackager.json',json.loads(z.read('assets/create/blockstates/repackager.json')))
save('assets/ccd/models/item/fluid_repackager.json',{'parent':'create:block/repackager/item'})
save('data/ccd/loot_table/blocks/fluid_repackager.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'ccd:fluid_repackager'}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
save('data/ccd/recipe/fluid_repackager.json',{'type':'minecraft:crafting_shapeless','category':'redstone','ingredients':[{'item':'create:repackager'},{'item':'create:fluid_pipe'},{'item':'create:electron_tube'}],'result':{'id':'ccd:fluid_repackager','count':1}})
path=res/'data/minecraft/tags/block/mineable/pickaxe.json'
tag=json.loads(path.read_text(encoding='utf-8'))
if 'ccd:fluid_repackager' not in tag['values']: tag['values'].append('ccd:fluid_repackager')
save('data/minecraft/tags/block/mineable/pickaxe.json',tag)
for locale in ('zh_cn','en_us'):
    path=res/f'assets/ccd/lang/{locale}.json';data=json.loads(path.read_text(encoding='utf-8'))
    data['block.ccd.fluid_repackager']='流体理包机' if locale=='zh_cn' else 'Fluid Repackager'
    data['block.ccd.fluid_repackager.tooltip.summary']='将同一订单的_物品与流体_合并为黄铜包裹。' if locale=='zh_cn' else 'Combines _Items and Fluids_ from the same order into Brass Packages.'
    data['ccd.ponder.fluid_packaging.text_6']='流体理包机可将同一订单的物品和流体合并为黄铜包裹' if locale=='zh_cn' else 'A Fluid Repackager combines items and fluids from the same order into Brass Packages'
    save(f'assets/ccd/lang/{locale}.json',data)
