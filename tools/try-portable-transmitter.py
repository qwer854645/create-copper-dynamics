"""Back up the transmitter and try native portable-fluid-interface fittings."""
import copy
import json
import shutil
from pathlib import Path
from zipfile import ZipFile

ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'src/main/resources/assets/ccd'
BACKUP=ROOT/'outputs/portable-transmitter/backup'
BACKUP.mkdir(parents=True,exist_ok=True)
for relative in ('models/block/fluid_transmitter.json','models/item/fluid_transmitter.json','blockstates/fluid_transmitter.json'):
    target=BACKUP/relative
    if not target.exists():
        target.parent.mkdir(parents=True,exist_ok=True)
        shutil.copy2(ASSETS/relative,target)
target=BACKUP/'textures/block/fluid_transmitter'
if not target.exists(): shutil.copytree(ASSETS/'textures/block/fluid_transmitter',target)
jar=next((Path.home()/'.gradle/caches/modules-2/files-2.1/com.simibubi.create/create-1.21.1/6.0.10-280').rglob('*-slim.jar'))
with ZipFile(jar) as archive:
    native=json.loads(archive.read('assets/create/models/block/portable_fluid_interface/block_top.json'))
textures={'body':'create:block/copper_underside','particle':'#body'}
elements=[]
faces={side:{'texture':'#body','uv':[0,0,16,16]} for side in ('north','south','east','west','up','down')}
elements.append({'name':'copper_body','from':[0,0,3],'to':[16,16,13],'faces':faces})
# Compress only the telescope length, preserving native end dimensions and UVs.
for positive in (False,True):
    remap={'up':'south' if positive else 'north','down':'north' if positive else 'south',
           'north':'up','south':'down','east':'east','west':'west'}
    for index,source in enumerate(native['elements']):
        e=copy.deepcopy(source)
        def point(p):
            return [p[0],16-p[2],13+(p[1]-7)/3 if positive else 3-(p[1]-7)/3]
        a,b=point(e['from']),point(e['to'])
        e['from']=[min(x,y) for x,y in zip(a,b)]
        e['to']=[max(x,y) for x,y in zip(a,b)]
        e.pop('rotation',None)
        e['name']=('output_' if positive else 'input_')+str(index)
        e['faces']={remap[side]:{**face,'texture':'#interface'} for side,face in e['faces'].items()}
        elements.append(e)
textures['interface']='create:block/portable_fluid_interface'
model={'parent':'minecraft:block/block','render_type':'minecraft:cutout_mipped','textures':textures,'elements':elements}
(ASSETS/'models/block/fluid_transmitter.json').write_text(json.dumps(model,indent=2)+'\n',encoding='utf-8')
print('Original assets backed up under outputs/portable-transmitter/backup; no textures regenerated.')
