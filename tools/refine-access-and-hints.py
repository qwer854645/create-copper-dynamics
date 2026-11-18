"""Correct CCD socket UVs and remove usage already taught by CCD Ponder."""
import json
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'src/main/resources/assets/ccd'
def save(path,data):
    path.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')

for name in ('fluid_access_port','fluid_bridge'):
    path=ASSETS/f'models/block/{name}.json'
    model=json.loads(path.read_text(encoding='utf-8'))
    for element in model['elements']:
        if element.get('name','').startswith('raised_socket'):
            # Fixed ten-pixel square socket and native front atlas strips.
            south=element['from'][2]>8
            element['from']=[2.95,2.95,15.9 if south else -1.95]
            element['to']=[13.05,13.05,17.95 if south else .1]
            element['faces']={
                'south' if south else 'north':{'texture':'#socket','uv':[3,3,13,13]},
                'east':{'texture':'#socket','uv':[13,3,11,13]},
                'west':{'texture':'#socket','uv':[5,3,3,13]},
                'up':{'texture':'#socket','uv':[13,3,3,5]},
                'down':{'texture':'#socket','uv':[13,11,3,13]}}
    save(path,model)

# Remove only sections actually covered by each registered storyboard, then
# renumber remaining pairs so Create continues to display them.
covered={
 'fluid_packager':{'condition1','condition2','control1'},
 'fluid_arm':{'condition1','control1'},
 'fluid_hatch':{'condition1','control1'},
 'smart_fluid_hatch':{'condition1','condition2','control1'},
 'smart_fluid_tank':{'condition1','control1'},
 'horizontal_smart_fluid_tank':{'condition1','control1'},
 'faucet':{'condition1','control1'},
 'smart_faucet':{'condition1','condition2','control1'},
 'fluid_access_port':{'condition1'},
 'fluid_bridge':{'condition1','control1'},
 'fluid_atomizer':{'condition1','condition3','control1'},
 'three_way_valve':{'condition1','condition2'},
 'copper_collection_grate':{'condition1'},
 'smart_collection_grate':{'condition1','condition2','condition3'},
 'fluid_transmitter':{'condition1','condition2','control1'},
 'booster_pump':{'condition1','control1'},
}
for locale in ('zh_cn','en_us'):
    path=ASSETS/f'lang/{locale}.json';data=json.loads(path.read_text(encoding='utf-8'))
    for name,sections in covered.items():
        prefix=f'block.ccd.{name}.tooltip.'
        for first,second in (('condition','behaviour'),('control','action')):
            remaining=[]
            for index in range(1,10):
                key=first+str(index)
                if prefix+key not in data: continue
                pair=(data.pop(prefix+key),data.pop(prefix+second+str(index)))
                if key not in sections: remaining.append(pair)
            for index,pair in enumerate(remaining,1):
                data[prefix+first+str(index)],data[prefix+second+str(index)]=pair
    data['ccd.hint.fluid_arm_no_targets']=('动力移液臂未设置取液点或注液点' if locale=='zh_cn'
        else 'No inputs or outputs configured for this Mechanical Fluid Arm')
    save(path,data)
