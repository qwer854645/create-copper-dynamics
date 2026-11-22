"""Generate only the five new media; preserve all previously approved artwork."""
import json
from pathlib import Path
from zipfile import ZipFile
from io import BytesIO
from PIL import Image

root=Path(__file__).resolve().parents[1]
resources=root/'src/main/resources'
media=[
 ('withering_fluid','凋零液','Withering Fluid','withering','minecraft:wither_rose','423448','凋零','withering','施加_凋零_。','Applies _Wither_.'),
 ('conduit_energy_fluid','潮涌能量液','Conduit Energy Fluid','tidal','minecraft:nautilus_shell','46cabb','海潮','tidal','给予_水下呼吸_和_急迫_。','Grants _Water Breathing_ and _Haste_.'),
 ('moss_suspension','苔藓悬浊液','Moss Suspension','mossify','minecraft:moss_block','668b36','苔化','mossifying','施加_缓慢_。','Applies _Slowness_.'),
 ('tear_solution','催泪溶液','Lachrymatory Solution','tear','minecraft:crying_obsidian','985de0','催泪','tear','施加_失明_和_虚弱_。','Applies _Blindness_ and _Weakness_.'),
 ('corrupting_fluid','腐化液','Corrupting Fluid','corrupted',None,'8d294e','腐化','corrupting','施加_凋零_和_虚弱_。','Applies _Wither_ and _Weakness_.')]
def save(path,data):
    path=resources/path;path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
jar=next((root/'build/moddev/artifacts').glob('*minecraft-resources.jar'))
with ZipFile(jar) as z:
    empty=Image.open(BytesIO(z.read('assets/minecraft/textures/item/bucket.png'))).convert('RGBA')
    water=Image.open(BytesIO(z.read('assets/minecraft/textures/item/water_bucket.png'))).convert('RGBA')
langs={locale:json.loads((resources/f'assets/ccd/lang/{locale}.json').read_text(encoding='utf-8')) for locale in ['zh_cn','en_us']}
for id,zh,en,processing,ingredient,color,zh_action,en_action,zh_effect,en_effect in media:
    save(Path(f'assets/ccd/blockstates/{id}.json'),{'variants':{'':{'model':'minecraft:block/water'}}})
    save(Path(f'assets/ccd/models/item/{id}_bucket.json'),{'parent':'minecraft:item/generated','textures':{'layer0':f'ccd:item/{id}_bucket'}})
    save(Path(f'data/create_more_catalysts/tags/fluid/fan_catalysts/{processing}.json'),{'replace':False,'values':[f'ccd:{id}',f'ccd:flowing_{id}']})
    conditions=[{'type':'neoforge:mod_loaded','modid':'create_more_catalysts'}]
    ingredients=[{'type':'neoforge:single','fluid':'minecraft:water','amount':1000}]
    if ingredient:ingredients.append({'item':ingredient})
    if id=='conduit_energy_fluid':ingredients.append({'item':'minecraft:prismarine_crystals'})
    if id=='corrupting_fluid':
        # Both supported companion mods have an obtainable route, with no missing item references.
        conditions.append({'type':'neoforge:or','values':[{'type':'neoforge:mod_loaded','modid':'anvilcraft'},{'type':'neoforge:mod_loaded','modid':'l2hostility'}]})
        ingredients.extend([{'type':'neoforge:single','fluid':'ccd:withering_fluid','amount':250},{'item':'minecraft:echo_shard'},{'item':'minecraft:fermented_spider_eye'}])
    save(Path(f'data/ccd/recipe/mixing/{id}.json'),{'type':'create:mixing','neoforge:conditions':conditions,'ingredients':ingredients,'results':[{'id':f'ccd:{id}','amount':1000}]})
    icon=empty.copy();rgb=bytes.fromhex(color)
    for y in range(16):
        for x in range(16):
            pixel=water.getpixel((x,y))
            if pixel!=empty.getpixel((x,y)):
                light=max(pixel[:3])/255
                icon.putpixel((x,y),tuple(round(c*(.7+.3*light)) for c in rgb)+(pixel[3],))
    icon.save(resources/f'assets/ccd/textures/item/{id}_bucket.png')
    for locale,data in langs.items():
        chinese=locale=='zh_cn';name=zh if chinese else en;action=zh_action if chinese else en_action
        data.update({f'fluid_type.ccd.{id}':name,f'fluid.ccd.{id}':name,f'fluid.ccd.flowing_{id}':name,f'block.ccd.{id}':name,
            f'item.ccd.{id}_bucket':name+('桶' if chinese else ' Bucket'),
            f'item.ccd.{id}_bucket.tooltip.summary':f'用于_{action}_的流体。' if chinese else f'A fluid for _{action}_ processing.',
            f'item.ccd.{id}_bucket.tooltip.condition1':'安装对应加工附属并装入雾化器时' if chinese else 'When used in an Atomizer with the matching processing addon',
            f'item.ccd.{id}_bucket.tooltip.behaviour1':f'使气流获得_{action}_加工效果。' if chinese else f'Applies _{action}_ processing to the air current.',
            f'item.ccd.{id}_bucket.tooltip.condition2':'接触流体或受其气流影响时' if chinese else 'When touching the fluid or exposed to its air current',
            f'item.ccd.{id}_bucket.tooltip.behaviour2':(zh_effect+'雾化后效果_更强、持续更短_。') if chinese else (en_effect+' Atomized effects are _stronger and shorter_.'),
        })
        if id=='corrupting_fluid':data[f'item.ccd.{id}_bucket.tooltip.condition1']='安装更多触媒及铁砧工艺或 L2 敌对并装入雾化器时' if chinese else 'With More Catalysts and AnvilCraft or L2 Hostility installed, in an Atomizer'
for locale,data in langs.items():save(Path(f'assets/ccd/lang/{locale}.json'),data)
print('Generated five media: tags, mixing recipes, static buckets, bilingual names and tooltips.')
