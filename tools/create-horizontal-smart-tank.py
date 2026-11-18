"""Horizontal tank uses existing brass tank materials and rotated native geometry."""
import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
RES=ROOT/'src/main/resources'
def save(p,d):
    p.parent.mkdir(parents=True,exist_ok=True)
    p.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
variants={}
for axis in ['x','z']:
    for bottom in [False,True]:
        for top in [False,True]:
            for shape in ['plain','window','window_ne','window_nw','window_se','window_sw']:
                part='single' if bottom and top else 'bottom' if bottom else 'top' if top else 'middle'
                model='block_'+part+('' if shape=='plain' else '_'+shape)
                variants[f'axis={axis},bottom={str(bottom).lower()},shape={shape},top={str(top).lower()}']={
                    'model':f'ccd:block/smart_fluid_tank/{model}','x':90,'y':90 if axis=='x' else 0}
save(RES/'assets/ccd/blockstates/horizontal_smart_fluid_tank.json',{'variants':variants})
save(RES/'assets/ccd/models/item/horizontal_smart_fluid_tank.json',{'parent':'ccd:block/smart_fluid_tank/block_single_window','display':{'gui':{'rotation':[30,45,90],'scale':[.625,.625,.625]}}})
save(RES/'data/ccd/loot_table/blocks/horizontal_smart_fluid_tank.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'ccd:horizontal_smart_fluid_tank'}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
for source,target in [('smart_fluid_tank','horizontal_smart_fluid_tank'),('horizontal_smart_fluid_tank','smart_fluid_tank')]:
    save(RES/f'data/ccd/recipe/{target}_conversion.json',{'type':'minecraft:crafting_shapeless','ingredients':[{'item':'ccd:'+source}],'result':{'id':'ccd:'+target,'count':1}})
for locale,name in [('zh_cn','横向智能储罐'),('en_us','Horizontal Smart Fluid Tank')]:
    path=RES/f'assets/ccd/lang/{locale}.json'; d=json.loads(path.read_text(encoding='utf-8')); d['block.ccd.horizontal_smart_fluid_tank']=name; save(path,d)
path=RES/'data/minecraft/tags/block/mineable/pickaxe.json'; d=json.loads(path.read_text(encoding='utf-8'))
if 'ccd:horizontal_smart_fluid_tank' not in d['values']: d['values'].append('ccd:horizontal_smart_fluid_tank')
save(path,d)
