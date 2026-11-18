"""CCD reagent recipes and upstream tag bindings, preserving upstream recipe types."""
import json
from pathlib import Path
from PIL import Image
ROOT=Path(__file__).resolve().parents[1]/'src/main/resources'
def save(p,d):
    p.parent.mkdir(parents=True,exist_ok=True); p.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
media=[
('seething_reagent','超热介质','Seething Reagent','minecraft:blaze_powder','dndesires','fan_processing_catalysts/seething'),
('enriching_reagent','富集介质','Enriching Reagent','#c:dusts/uranium','ccd','fan_media/enriched'),
('soul_stripping_reagent','剥魂液','Soul Stripping Reagent','minecraft:soul_sand','ccd','fan_media/soul_stripping'),
('explosive_slurry','爆破浆液','Explosive Slurry','minecraft:gunpowder','create_more_catalysts','fan_catalysts/exploding'),
('resonant_solution','共鸣溶液','Resonant Solution','minecraft:amethyst_shard','create_more_catalysts','fan_catalysts/resonance'),
('sculk_suspension','幽匿悬液','Sculk Suspension','minecraft:sculk','create_more_catalysts','fan_catalysts/sculking'),
('purifying_solution','净化液','Purifying Solution','minecraft:glowstone_dust','create_more_catalysts','fan_catalysts/purifying')]
for id,zh,en,ingredient,mod,tag in media:
    save(ROOT/f'assets/ccd/blockstates/{id}.json',{'variants':{'':{'model':'minecraft:block/water'}}})
    save(ROOT/f'assets/ccd/models/item/{id}_bucket.json',{'parent':'minecraft:item/generated','textures':{'layer0':'ccd:item/'+id+'_bucket'}})
    save(ROOT/f'data/{mod}/tags/fluid/{tag}.json',{'replace':False,'values':['ccd:'+id,'ccd:flowing_'+id]})
    recipe={'type':'create:mixing','ingredients':[{'type':'neoforge:single','fluid':'minecraft:water','amount':1000},
        {'tag':ingredient[1:]} if ingredient.startswith('#') else {'item':ingredient}],
        'results':[{'id':'ccd:'+id,'amount':1000}]}
    if id=='seething_reagent': recipe['heat_requirement']='superheated'
    required={'seething_reagent':'dndesires','enriching_reagent':'createnuclear','soul_stripping_reagent':'createnetherindustry'}.get(id,'create_more_catalysts')
    recipe['neoforge:conditions']=[{'type':'neoforge:mod_loaded','modid':required}]
    save(ROOT/f'data/ccd/recipe/mixing/{id}.json',recipe)
    for locale,name in [('zh_cn',zh),('en_us',en)]:
        p=ROOT/f'assets/ccd/lang/{locale}.json'; d=json.loads(p.read_text(encoding='utf-8'))
        d.update({'fluid_type.ccd.'+id:name,'fluid.ccd.'+id:name,'fluid.ccd.flowing_'+id:name,'block.ccd.'+id:name,
            'item.ccd.'+id+'_bucket':name+'桶' if locale=='zh_cn' else name+' Bucket'})
        save(p,d)
for name,size in [('processing_medium_still',16),('processing_medium_flow',32)]:
    im=Image.new('RGBA',(size,size*4))
    for frame in range(4):
        for y in range(size):
            for x in range(size):
                shade=[180,198,216,232][((x//4)+(y+frame)//3+(x+y)//9)%4]
                im.putpixel((x,y+frame*size),(shade,shade,shade,255))
    p=ROOT/f'assets/ccd/textures/block/{name}.png'; im.save(p); save(p.with_suffix('.png.mcmeta'),{'animation':{'frametime':8}})
