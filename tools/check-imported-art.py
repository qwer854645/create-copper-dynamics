"""Read-only reference checks for the imported aaa (1).zip model set."""
import json
from pathlib import Path
from zipfile import ZipFile

ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'src/main/resources/assets'
jar=next((Path.home()/'.gradle/caches/modules-2/files-2.1/com.simibubi.create/create-1.21.1/6.0.10-280').rglob('*-slim.jar'))
errors=[]
with ZipFile(jar) as archive:
    def load(identifier):
        namespace,name=identifier.split(':',1) if ':' in identifier else ('minecraft',identifier)
        relative=f'{namespace}/models/{name}.json'
        path=ASSETS/relative
        if path.exists(): return json.loads(path.read_text(encoding='utf-8'))
        key='assets/'+relative
        if key in archive.namelist(): return json.loads(archive.read(key))
        if identifier in ('block/block','minecraft:block/block'): return {}
        return {}
    def textures(model,visited=None):
        visited=set() if visited is None else visited
        parent=model.get('parent')
        result={}
        if parent and parent not in visited:
            visited.add(parent);result.update(textures(load(parent),visited))
        result.update(model.get('textures',{}));return result
    for path in (ASSETS/'ccd/models').rglob('*.json'):
        model=json.loads(path.read_text(encoding='utf-8'));mapping=textures(model)
        for element in model.get('elements',[]):
            for face in element['faces'].values():
                value=face['texture'];seen=set()
                if not value.startswith('#'):
                    errors.append((str(path.relative_to(ROOT)),'Face texture must reference a #variable: '+value))
                while value.startswith('#') and value not in seen:
                    seen.add(value);value=mapping.get(value[1:],'MISSING:'+value)
                if value.startswith(('MISSING:','#')): errors.append((str(path.relative_to(ROOT)),value))
                elif value.startswith('ccd:') and not (ASSETS/'ccd/textures'/(value.split(':',1)[1]+'.png')).exists():
                    errors.append((str(path.relative_to(ROOT)),value))
assert not errors,errors
print('All CCD model texture variables and CCD texture paths resolve.')
