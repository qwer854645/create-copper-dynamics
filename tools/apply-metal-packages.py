"""Apply approved package skins and add the three additional sizes per material."""
import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
A=ROOT/'src/main/resources/assets/ccd'
for material,base in [('copper','fluid_package'),('brass','composite_package')]:
    for size in ['10x8','12x10','10x12','12x12']:
        name=base if size=='12x12' else material+'_package_'+size
        model=json.loads((A/f'models/item/native_package_{material}_{size}_study.json').read_text())
        (A/f'models/item/{name}.json').write_text(json.dumps(model,indent=2)+'\n')
for locale,names in [('zh_cn',['铜包裹','黄铜包裹']),('en_us',['Copper Package','Brass Package'])]:
    p=A/f'lang/{locale}.json'; data=json.loads(p.read_text(encoding='utf-8'))
    data['item.ccd.fluid_package']=names[0]; data['item.ccd.composite_package']=names[1]
    if locale=='zh_cn':
        data={k:v.replace('黄铜盒','黄铜包裹').replace('密封流体罐','铜包裹') for k,v in data.items()}
    else:
        data={k:v.replace('Brass Box','Brass Package').replace('sealed fluid canisters','copper packages') for k,v in data.items()}
    p.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
