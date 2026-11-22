"""Resolve actual blockstate models and parents before assigning missing particle sprites."""
from pathlib import Path
from zipfile import ZipFile
import json
root=Path(__file__).resolve().parents[1]
resources=root/'src/main/resources'
create=ZipFile(next((Path.home()/'.gradle/caches/modules-2/files-2.1/com.simibubi.create/create-1.21.1/6.0.10-280').rglob('*-slim.jar')))
vanilla=ZipFile(next((root/'build/moddev/artifacts').glob('*minecraft-resources.jar')))
def location(id,kind,ext):
    ns,name=id.split(':') if ':' in id else ('minecraft',id)
    return f'assets/{ns}/{kind}/{name}{ext}'
def load(id):
    path=location(id,'models','.json');p=resources/path
    if p.exists():return json.loads(p.read_text())
    for jar in [create,vanilla]:
        if path in jar.namelist():return json.loads(jar.read(path))
    raise FileNotFoundError(path)
def textures(id,seen=()):
    if id in seen:raise ValueError('Model parent cycle '+id)
    d=load(id);parent=d.get('parent')
    inherited=textures(parent,(*seen,id)) if parent and not parent.startswith('builtin/') else {}
    return {**inherited,**d.get('textures',{})}
def resolve(value,tex):
    seen=set()
    while isinstance(value,str) and value.startswith('#'):
        if value in seen:return None
        seen.add(value);value=tex.get(value[1:])
    if not value:return None
    path=location(value,'textures','.png')
    return value if (resources/path).exists() or path in create.namelist() or path in vanilla.namelist() else None
models=set()
def walk(value):
    if isinstance(value,dict):
        if 'model' in value:models.add(value['model'])
        for v in value.values():walk(v)
    elif isinstance(value,list):
        for v in value:walk(v)
for p in (resources/'assets/ccd/blockstates').glob('*.json'):walk(json.loads(p.read_text()))
changed=[]
for id in sorted(models):
    tex=textures(id)
    if resolve(tex.get('particle'),tex):continue
    candidates=[resolve(v,tex) for k,v in tex.items() if k!='particle']
    chosen=next((v for v in candidates if v),None)
    if not chosen:raise ValueError('No usable particle for '+id)
    p=resources/location(id,'models','.json')
    if not p.exists():raise ValueError('Cannot edit upstream model '+id)
    d=json.loads(p.read_text());d.setdefault('textures',{})['particle']=chosen
    p.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n',encoding='utf-8');changed.append((id,chosen))
for id in models:assert resolve(textures(id).get('particle'),textures(id)),id
print('Checked',len(models),'blockstate model references; fixed',len(changed))
for id,sprite in changed:print(id,'->',sprite)
