"""Restore native rotor geometry only, preserving current user-edited materials."""
import json
from pathlib import Path
from zipfile import ZipFile

root=Path(__file__).resolve().parents[1]
models=root/'src/main/resources/assets/ccd/models'
jar=next((Path.home()/'.gradle/caches/modules-2/files-2.1/com.simibubi.create/create-1.21.1/6.0.10-280').rglob('*-slim.jar'))
with ZipFile(jar) as archive:
    rotor=json.loads(archive.read('assets/create/models/block/encased_fan/propeller.json'))
    native_item=json.loads(archive.read('assets/create/models/block/encased_fan/item.json'))
path=models/'block/fluid_atomizer/propeller.json'
current=json.loads(path.read_text(encoding='utf-8'))
current['elements']=rotor['elements']
path.write_text(json.dumps(current,indent=2)+'\n',encoding='utf-8')
path=models/'item/fluid_atomizer.json'
current=json.loads(path.read_text(encoding='utf-8'))
native={part.get('name'):part for part in native_item['elements'] if part.get('name') in ('Fan','Shaft')}
for index,part in enumerate(current['elements']):
    if part.get('name') in native:
        current['elements'][index]=native[part['name']]
path.write_text(json.dumps(current,indent=2)+'\n',encoding='utf-8')
fan=next(part for part in rotor['elements'] if part.get('name')=='Fan')
assert fan['from'][2]==4 and fan['to'][2]==12
print('Native rotor restored: blade plane z=4; native bounds z=4..12; materials untouched.')
