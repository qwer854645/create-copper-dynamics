"""Functional prototype resources, using existing Create materials without custom art."""
import json
import runpy
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
RES=ROOT/'src/main/resources'
def save(path,value):
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')

for name,bridge in [('fluid_access_port',False),('fluid_bridge',True)]:
    textures={'shell':'create:block/brass_casing' if bridge else 'create:block/copper_casing',
              'connection':'create:block/industrial_iron_block','particle':'#shell'}
    elements=[]
    for lo,hi,texture in [([2,2,0],[14,14,16],'shell'),([4,4,15],[12,12,16],'connection')]+([([4,4,0],[12,12,1],'connection')] if bridge else []):
        elements.append({'from':lo,'to':hi,'faces':{side:{'texture':'#'+texture,'uv':[0,0,16,16]} for side in ['north','south','east','west','up','down']}})
    save(RES/f'assets/ccd/models/block/{name}.json',{'parent':'minecraft:block/block','textures':textures,'elements':elements})
    save(RES/f'assets/ccd/models/item/{name}.json',{'parent':f'ccd:block/{name}'})
    variants={}
    for side,x,y in [('north',0,0),('south',0,180),('east',0,90),('west',0,270),('up',270,0),('down',90,0)]:
        variants[f'facing={side}']={'model':f'ccd:block/{name}','x':x,'y':y}
    save(RES/f'assets/ccd/blockstates/{name}.json',{'variants':variants})
    save(RES/f'data/ccd/loot_table/blocks/{name}.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':f'ccd:{name}'}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
    ingredients=([{'item':'create:brass_casing'},{'item':'ccd:fluid_access_port'},{'item':'ccd:fluid_access_port'},{'item':'create:electron_tube'},{'item':'create:electron_tube'}] if bridge else [{'item':'create:copper_casing'},{'item':'create:fluid_pipe'},{'item':'create:fluid_pipe'}])
    save(RES/f'data/ccd/recipe/{name}.json',{'type':'minecraft:crafting_shapeless','ingredients':ingredients,'result':{'id':f'ccd:{name}','count':1 if bridge else 2}})
tag=RES/'data/minecraft/tags/block/mineable/pickaxe.json'
data=json.loads(tag.read_text(encoding='utf-8'))
for name in ['fluid_access_port','fluid_bridge']:
    if f'ccd:{name}' not in data['values']: data['values'].append(f'ccd:{name}')
save(tag,data)
for locale,entries in {
    'zh_cn':{'block.ccd.fluid_access_port':'流体扩展端口','block.ccd.fluid_bridge':'流体桥接器',
             'ccd.fluid_access.sources':'已连接 %s 个容器','ccd.fluid_access.no_storage':'无内部储液；由外部设备驱动存取',
             'block.ccd.fluid_access_port.tooltip.summary':'扩展后方容器的_流体访问面_，不主动输送或储液。',
             'block.ccd.fluid_bridge.tooltip.summary':'通过侧面访问两端容器，端面过滤限制_流体读写_。'},
    'en_us':{'block.ccd.fluid_access_port':'Fluid Access Port','block.ccd.fluid_bridge':'Fluid Bridge',
             'ccd.fluid_access.sources':'Connected containers: %s','ccd.fluid_access.no_storage':'No internal storage; external devices perform transfers',
             'block.ccd.fluid_access_port.tooltip.summary':'Extends the attached container\'s _fluid access faces_ without pumping or storage.',
             'block.ccd.fluid_bridge.tooltip.summary':'Access both end containers from the sides. End filters restrict _fluid reads and transfers_.'}
}.items():
    file=RES/f'assets/ccd/lang/{locale}.json'
    language=json.loads(file.read_text(encoding='utf-8')); language.update(entries); save(file,language)

# Regenerating functional resources must preserve the current art models.
runpy.run_path(str(ROOT/'tools/create-fluid-access-art.py'),run_name='__main__')
runpy.run_path(str(ROOT/'tools/update-fluid-access-text.py'),run_name='__main__')
