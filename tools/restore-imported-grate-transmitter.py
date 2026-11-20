"""Restore the user's imported revision, not the pre-import backup."""
import copy,json
from pathlib import Path
from zipfile import ZipFile
root=Path(__file__).resolve().parents[1]
a=root/'src/main/resources/assets/ccd'
def save(p,d):p.write_text(json.dumps(d,indent=2)+'\n',encoding='utf-8')
with ZipFile(Path.home()/'Downloads/aaa (1).zip') as archive:
    for relative in ('models/block/smart_collection_grate.json','models/item/smart_collection_grate.json',
                     'models/block/fluid_transmitter.json'):
        (a/relative).write_bytes(archive.read('aaa/assets/ccd/'+relative))
    for name in archive.namelist():
        if name.endswith('.png') and any('/textures/block/'+folder+'/' in name for folder in ('smart_collection_grate','fluid_transmitter')):
            (a/name.removeprefix('aaa/assets/ccd/')).write_bytes(archive.read(name))
path=a/'models/block/smart_collection_grate_filter.json';panel=json.loads(path.read_text())
element=panel['elements'][0];element['from']=[5.5,8.5,15.9];element['to']=[10.5,13.5,16.5]
save(path,panel)
path=a/'models/item/smart_collection_grate.json';item=json.loads(path.read_text())
body=json.loads((a/'models/block/smart_collection_grate.json').read_text())
item['textures']={**body['textures'],'filter':'create:block/smart_pipe_2'}
item['elements']=copy.deepcopy(body['elements'])
directions=['south','west','north','east']
for angle in (0,90,180,270):
    part=copy.deepcopy(element)
    def turn(p):
        x,y,z=p
        for _ in range(angle//90):x,z=16-z,x
        return [x,y,z]
    lo,hi=turn(element['from']),turn(element['to'])
    part['from']=[min(x,y) for x,y in zip(lo,hi)];part['to']=[max(x,y) for x,y in zip(lo,hi)]
    part['faces']={(directions[(directions.index(side)+angle//90)%4] if side in directions else side):face
                   for side,face in part['faces'].items()}
    part['name']='filter_'+str(angle);item['elements'].append(part)
save(path,item)
