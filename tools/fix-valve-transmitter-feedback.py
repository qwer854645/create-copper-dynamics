"""Correct live visual indicators without changing fluid routing."""
import json
from pathlib import Path
root=Path(__file__).resolve().parents[1]/'src/main/resources/assets/ccd'
def save(p,d):p.write_text(json.dumps(d,indent=2)+'\n',encoding='utf-8')
path=root/'models/block/fluid_transmitter.json';model=json.loads(path.read_text())
for element in model['elements']:
    if element.get('name')=='lower_bowl':
        for face in element['faces'].values():
            if face['texture']=='#side': face['rotation']=(face.get('rotation',0)+180)%360
save(path,model)
path=root/'blockstates/three_way_valve.json';states=json.loads(path.read_text())
for part in states['multipart']:
    identifier=part['apply']['model']
    if '/dial_' not in identifier and identifier.startswith('ccd:block/three_way_valve/'):
        # Each physical port has a permanent colour: common, straight, branch.
        when=part.get('when',{})
        if 'powered' in when:
            if when['powered']=='false':
                del when['powered']
            else: part['_remove']=True
states['multipart']=[p for p in states['multipart'] if not p.pop('_remove',False)]
save(path,states)
for state in ('powered','unpowered'):
    path=root/f'models/block/three_way_valve/dial_{state}.json';model=json.loads(path.read_text())
    model['textures']['needle']='create:block/fluid_valve'
    pointer=next(e for e in model['elements'] if e['name']=='pointer')
    pointer['from']=[7.5,13.07,4.5];pointer['to']=[8.5,13.07,11.5]
    pointer['faces']={'up':{'texture':'#needle','uv':[2,7.5,5.5,8],'rotation':270}}
    if state=='powered':
        pointer['from']=[4.5,13.07,7.5];pointer['to']=[11.5,13.07,8.5]
        pointer['faces']['up']['rotation']=0
    save(path,model)
# Synchronize item needles with the same native artwork.
path=root/'models/item/three_way_valve.json';model=json.loads(path.read_text());model['textures']['needle']='create:block/fluid_valve'
for element in model['elements']:
    if element.get('name','').endswith('_pointer'):
        for face in element['faces'].values():face.update(uv=[2,7.5,5.5,8],rotation=270)
save(path,model)
