"""Compare independent model matrices against the logical local frame in all poses."""
from pathlib import Path
import json,math
import numpy as np
root=Path(__file__).resolve().parents[1]/'src/main/resources/assets/ccd'
vectors={'north':np.array([0,0,-1]),'east':np.array([1,0,0]),'south':np.array([0,0,1]),'west':np.array([-1,0,0]),'up':np.array([0,1,0]),'down':np.array([0,-1,0])}
def matrix(axis,degrees):
    angle=math.radians(degrees);c=round(math.cos(angle));s=round(math.sin(angle))
    return {'x':np.array([[1,0,0],[0,c,-s],[0,s,c]]),'y':np.array([[c,0,s],[0,1,0],[-s,0,c]]),'z':np.array([[c,-s,0],[s,c,0],[0,0,1]])}[axis]
for pump in ['variable_flow_pump','redstone_fluid_pump']:
    states=json.loads((root/f'blockstates/{pump}.json').read_text())['variants'];assert len(states)==24
    for key,model in states.items():
        values=dict(part.split('=') for part in key.split(','));front=values['facing'];roll=int(values['roll'])
        body=json.loads((root/'models'/ (model['model'].split(':')[1]+'.json')).read_text())
        local=matrix('z',-90*roll)
        if roll:assert body['transform']['origin']=='center' and body['transform']['rotation']['z']==-90*roll
        baked=matrix('y',-model.get('y',0))@matrix('x',-model.get('x',0))@local
        assert np.array_equal(baked@vectors['north'],vectors[front]),(pump,key,'pipe')
        expected=matrix('x',90 if front=='up' else -90) if front in ['up','down'] else matrix('y',{'north':0,'east':-90,'south':180,'west':90}[front])
        renderer=expected@local
        assert np.array_equal(baked,renderer),(pump,key,'renderer')
print('Verified 48 static/dynamic frame matrices, pipe facings, roll pivots and shaft/contact axes.')
