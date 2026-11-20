"""Repair format/reference issues in user models without repainting textures."""
import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
M=ROOT/'src/main/resources/assets/ccd/models'
def save(path,data): path.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
path=M/'block/redstone_fluid_valve/block_horizontal_closed.json'
d=json.loads(path.read_text());d['textures']['missing']='create:block/fluid_valve';save(path,d)
mapping={'north':'down','south':'up','up':'north','down':'south','east':'east','west':'west'}
for name in ('fluid_atomizer','redstone_fluid_valve'):
    path=M/f'item/{name}.json';d=json.loads(path.read_text())
    for e in d['elements']:
        rotation=e.get('rotation',{})
        if 'x' not in rotation: continue
        assert rotation['x']==-90 and rotation['y']==rotation['z']==0
        ox,oy,oz=rotation['origin']
        def turn(p): return [p[0],oy+p[2]-oz,oz-p[1]+oy]
        # Preserve reversed endpoints used for inner faces.
        a,b=turn(e['from']),turn(e['to'])
        e['from']=[a[0],a[1],b[2]];e['to']=[b[0],b[1],a[2]]
        for side,face in e['faces'].items():
            if side in ('east','west'):
                face['rotation']=(face.get('rotation',0)+(90 if side=='east' else 270))%360
            elif side in ('south','down'):
                face['rotation']=(face.get('rotation',0)+180)%360
        e['faces']={mapping[side]:face for side,face in e['faces'].items()}
        del e['rotation']
    save(path,d)
