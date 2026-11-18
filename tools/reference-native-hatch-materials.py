"""Keep native hatch geometry; adapt UVs to directly referenced 16px materials."""
import json
from pathlib import Path
from zipfile import ZipFile

root=Path(__file__).resolve().parents[1]
jar=next((Path.home()/'.gradle/caches/modules-2/files-2.1/com.simibubi.create/create-1.21.1/6.0.10-280').rglob('*-slim.jar'))
with ZipFile(jar) as archive:
    geometry=json.loads(archive.read('assets/create/models/block/item_hatch/block_closed.json'))['elements']
for element in geometry:
    for face in element['faces'].values():
        face['uv']=[0,0,16,16]
        face.pop('rotation',None)
for name,body,lid in [('fluid_hatch','fluid_tank','copper_underside'),('smart_fluid_hatch','brass_casing','brass_block')]:
    model={'parent':'create:block/item_hatch/block_closed','textures':{
        '0':'create:block/'+body,'1':'create:block/'+lid,'particle':'create:block/'+body},'elements':geometry}
    (root/f'src/main/resources/assets/ccd/models/block/{name}_closed.json').write_text(json.dumps(model,indent=2)+'\n',encoding='utf-8')
