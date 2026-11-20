"""Native valve dial on a free face; two power-driven pointer positions."""
import json,copy
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]/'src/main/resources/assets/ccd'
def save(p,d):p.write_text(json.dumps(d,indent=2)+'\n',encoding='utf-8')
def plate(name,lo,hi,uv,texture):
    return {'name':name,'from':lo,'to':hi,'faces':{'up':{'texture':texture,'uv':uv}}}
for powered in (False,True):
    needle=([4.5,13.07,7.6],[8.5,13.07,8.4]) if powered else ([7.6,13.07,4.5],[8.4,13.07,8.5])
    model={'parent':'minecraft:block/block','ambientocclusion':False,'render_type':'minecraft:cutout_mipped',
           'textures':{'dial':'create:block/valve_open','needle':'create:block/industrial_iron_block','glass':'create:block/fluid_valve'},
           'elements':[plate('dial',[3,13.02,3],[13,13.02,13],[2,2,14,14],'#dial'),
                       plate('pointer',*needle,[0,0,4,1],'#needle'),
                       plate('glass',[3,13.10,3],[13,13.10,13],[1,1,7,7],'#glass')]}
    model['elements'][0]['faces']['up']['rotation']=90
    save(ROOT/f'models/block/three_way_valve/dial_{"powered" if powered else "unpowered"}.json',model)
path=ROOT/'blockstates/three_way_valve.json';states=json.loads(path.read_text())
states['multipart']=[p for p in states['multipart'] if '/dial_' not in p['apply']['model']]
vectors={'north':(0,0,-1),'south':(0,0,1),'east':(1,0,0),'west':(-1,0,0),'up':(0,1,0),'down':(0,-1,0)}
def clockwise(v,axis):
    x,y,z=v
    return (x,-z,y) if axis==0 else (z,y,-x) if axis==1 else (-y,x,z)
rotations={'up':{},'down':{'x':180},'north':{'x':90},'south':{'x':270},'east':{'x':90,'y':90},'west':{'x':90,'y':270}}
for facing,v in vectors.items():
    axis=next(i for i,n in enumerate(v) if n)
    branch=vectors['north'] if axis==1 else vectors['up']
    for roll in range(4):
        branch_axis=next(i for i,n in enumerate(branch) if n)
        free=next(i for i in range(3) if i not in (axis,branch_axis))
        sides=(('east','west'),('up','down'),('south','north'))[free]
        for side in sides:
            for powered in (False,True):
                states['multipart'].append({'when':{'facing':facing,'roll':str(roll),'powered':str(powered).lower()},
                    'apply':{'model':f'ccd:block/three_way_valve/dial_{"powered" if powered else "unpowered"}',**rotations[side]}})
        branch=clockwise(branch,axis)
save(path,states)
# Default item uses the same free east face as SOUTH/roll=0.
path=ROOT/'models/item/three_way_valve.json';item=json.loads(path.read_text())
dial=json.loads((ROOT/'models/block/three_way_valve/dial_unpowered.json').read_text())
item['textures'].update(dial['textures'])
item['elements']=[e for e in item['elements'] if not e.get('name','').startswith('dial_item_')]
for side in ('east','west'):
    for part in dial['elements']:
        e=copy.deepcopy(part);e['name']='dial_item_'+side+'_'+part['name']
        def turn(p):return [p[1],16-p[2],16-p[0]] if side=='east' else [16-p[1],16-p[2],p[0]]
        a,b=turn(e['from']),turn(e['to'])
        e['from']=[min(x,y) for x,y in zip(a,b)];e['to']=[max(x,y) for x,y in zip(a,b)]
        e['faces']={side:e['faces']['up']};item['elements'].append(e)
save(path,item)
