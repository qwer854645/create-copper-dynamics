"""Reuse Create cage OBJ/UVs, compressed native tank and native burner animation."""
from pathlib import Path
from zipfile import ZipFile
import json
root=Path(__file__).resolve().parents[1];res=root/'src/main/resources'
def save(path,data):
    p=res/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
jar=next((Path.home()/'.gradle/caches/modules-2/files-2.1/com.simibubi.create/create-1.21.1/6.0.10-280').rglob('*-slim.jar'))
with ZipFile(jar) as z:
    obj=z.read('assets/create/models/block/blaze_burner/blaze_cage.obj').decode()
    lines=[]
    for line in obj.splitlines():
        if line.startswith('v '):
            x,y,zz=map(float,line.split()[1:]);line=f'v {x:.6f} {0.25+y*0.75:.6f} {zz:.6f}'
        if line.startswith('mtllib '):line='mtllib create:models/block/blaze_burner/blaze_cage.mtl'
        lines.append(line)
    p=res/'assets/ccd/models/block/tank_blaze_burner/cage.obj';p.parent.mkdir(parents=True,exist_ok=True);p.write_text('\n'.join(lines)+'\n')
    tank=json.loads(z.read('assets/create/models/block/fluid_tank/block_single.json'))
    for e in tank['elements']:
        for key in ['from','to']:e[key][1]/=4
        if 'rotation'in e:e['rotation']['origin'][1]/=4
        for f in e['faces'].values():f.pop('cullface',None)
    tank.pop('groups',None)
    save('assets/ccd/models/block/tank_blaze_burner/tank.json',tank)
save('assets/ccd/models/block/tank_blaze_burner/cage.json',{'parent':'minecraft:block/block','loader':'neoforge:obj','flip_v':True,'model':'ccd:models/block/tank_blaze_burner/cage.obj','textures':{'0':'create:block/blaze_heater_brazier','particle':'create:block/dark_metal_block'}})
# Composite loader preserves the native OBJ rather than approximating it with cuboids.
save('assets/ccd/models/block/tank_blaze_burner/block.json',{'loader':'neoforge:composite','children':{'tank':{'parent':'ccd:block/tank_blaze_burner/tank'},'burner':{'parent':'ccd:block/tank_blaze_burner/cage'}},'render_type':'minecraft:cutout_mipped','textures':{'particle':'create:block/fluid_tank'}})
save('assets/ccd/blockstates/tank_blaze_burner.json',{'variants':{'':{'model':'ccd:block/tank_blaze_burner/block'}}})
save('assets/ccd/models/item/tank_blaze_burner.json',{'parent':'ccd:block/tank_blaze_burner/block','display':{'gui':{'rotation':[30,225,0],'scale':[.7,.7,.7]}}})
save('data/ccd/recipe/tank_blaze_burner.json',{'type':'minecraft:crafting_shapeless','ingredients':[{'item':'create:blaze_burner'},{'item':'create:fluid_tank'},{'item':'create:fluid_pipe'},{'item':'create:copper_sheet'}],'result':{'id':'ccd:tank_blaze_burner','count':1}})
save('data/ccd/loot_table/blocks/tank_blaze_burner.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'ccd:tank_blaze_burner'}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
for path in ['data/create/tags/block/wrench_pickup.json','data/minecraft/tags/block/mineable/pickaxe.json']:
    data=json.loads((res/path).read_text());
    if 'ccd:tank_blaze_burner' not in data['values']:data['values'].append('ccd:tank_blaze_burner')
    save(path,data)
for loc,name,summary,condition,behavior in [
 ('zh_cn','储罐烈焰人燃烧室','内置_4000 mB 储罐_的烈焰人燃烧室，保留原生供热与固体燃料交互。','通过管道或移液臂注入液体燃料时','储存单种燃料，每累计_1000 mB_按对应桶燃料自动供燃。移液臂可直接选择，无需舱口。'),
 ('en_us','Tank Blaze Burner','A Blaze Burner with a _4000 mB tank_, retaining native heating and solid fuel interactions.','When supplying liquid fuel through pipes or a Fluid Arm','Stores one fuel at a time and automatically burns _1000 mB_ as its corresponding bucket fuel. Fluid Arms can target it directly without a Hatch.')]:
    path=f'assets/ccd/lang/{loc}.json';data=json.loads((res/path).read_text(encoding='utf-8'));data.update({'block.ccd.tank_blaze_burner':name,'block.ccd.tank_blaze_burner.tooltip.summary':summary,'block.ccd.tank_blaze_burner.tooltip.condition1':condition,'block.ccd.tank_blaze_burner.tooltip.behaviour1':behavior});save(path,data)
print('Generated native-derived one-block tank burner resources.')
