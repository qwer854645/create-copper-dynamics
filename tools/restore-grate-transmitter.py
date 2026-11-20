"""Restore pre-import assets and fit a shared filter on four grate sides."""
import copy,json,shutil
from pathlib import Path
root=Path(__file__).resolve().parents[1]
a=root/'src/main/resources/assets/ccd'
b=root/'outputs/imported-art-rework/backup/ccd'
def save(p,d):p.write_text(json.dumps(d,indent=2)+'\n',encoding='utf-8')
for rel in ('models/block/smart_collection_grate.json','models/item/smart_collection_grate.json',
            'models/block/fluid_transmitter.json'):
    shutil.copy2(b/rel,a/rel)
for folder in ('smart_collection_grate','fluid_transmitter'):
    for path in (b/'textures/block'/folder).glob('*.png'):
        shutil.copy2(path,a/'textures/block'/folder/path.name)
# The old smart model inherited liner materials from the old ordinary bowl;
# declare them explicitly so restoring only the smart version stays independent.
path=a/'models/block/smart_collection_grate.json';body=json.loads(path.read_text())
body['textures'].update(copper='create:block/copper_underside',inner='ccd:block/copper_collection_grate/inner',
    grate='ccd:block/copper_collection_grate/grate',pipe='create:block/pipes')
save(path,body)
# Keep the attachment on the broad upper tier of the restored bowl, z=15.
path=a/'models/block/smart_collection_grate_filter.json';panel=json.loads(path.read_text())
e=panel['elements'][0];e['from']=[5.5,8.5,14.9];e['to']=[10.5,13.5,15.5]
save(path,panel)
save(a/'blockstates/smart_collection_grate.json',{'multipart':[
    {'apply':{'model':'ccd:block/smart_collection_grate'}},
    *[{'apply':{'model':'ccd:block/smart_collection_grate_filter',**({'y':angle} if angle else {})}}
      for angle in (0,90,180,270)]]})
path=a/'models/item/smart_collection_grate.json';item=json.loads(path.read_text())
body=json.loads((a/'models/block/smart_collection_grate.json').read_text())
item['textures']={**body['textures'],'filter':'create:block/smart_pipe_2'}
item['elements']=copy.deepcopy(body['elements'])
directions=['south','west','north','east']
for angle in (0,90,180,270):
    part=copy.deepcopy(e)
    def turn(p):
        x,y,z=p
        for _ in range(angle//90):x,z=16-z,x
        return [x,y,z]
    lo,hi=turn(e['from']),turn(e['to'])
    part['from']=[min(x,y) for x,y in zip(lo,hi)];part['to']=[max(x,y) for x,y in zip(lo,hi)]
    def face(side):
        return directions[(directions.index(side)+angle//90)%4] if side in directions else side
    part['faces']={face(side):spec for side,spec in part['faces'].items()}
    part['name']='filter_'+str(angle);item['elements'].append(part)
save(path,item)
# Both handle states must share exactly the same fixed geometry and UVs.
closed=json.loads((a/'models/block/faucet_closed.json').read_text())
path=a/'models/block/faucet_open.json';opened=json.loads(path.read_text())
opened['elements']=[copy.deepcopy(e) for e in closed['elements'] if not e.get('name','').startswith('handle_')]+[
    e for e in opened['elements'] if e.get('name','').startswith('handle_')]
save(path,opened)
