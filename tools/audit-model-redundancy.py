"""Read-only model reachability and exact duplicate audit; dynamic references are roots."""
import json,re,hashlib
from pathlib import Path
root=Path(__file__).resolve().parents[1]
assets=root/'src/main/resources/assets/ccd'
models={str(p.relative_to(assets/'models')).replace('\\','/')[:-5]:json.loads(p.read_text(encoding='utf-8')) for p in (assets/'models').rglob('*.json')}
roots={k for k in models if k.startswith('item/')}
def refs(value):
    if isinstance(value,dict):
        for k,v in value.items():
            if k in ('model','parent') and isinstance(v,str) and v.startswith('ccd:'):yield v[4:]
            else:yield from refs(v)
    elif isinstance(value,list):
        for v in value:yield from refs(v)
for p in (assets/'blockstates').glob('*.json'):roots.update(refs(json.loads(p.read_text())))
java='\n'.join(p.read_text(encoding='utf-8') for p in (root/'src/main/java').rglob('*.java'))
for k in models:
    if '"'+k+'"' in java:roots.add(k)
# Dynamic prefix constructors in the active renderers.
for prefix in ['block/portable_fluid_pump/','block/redstone_fluid_valve/','block/fluid_packager/','block/fluid_arm/']:
    for k in models:
        if k.startswith(prefix) and '"'+k.split('/')[-1]+'"' in java:roots.add(k)
seen=set()
def visit(k):
    if k in seen:return
    seen.add(k)
    if k in models:
        for ref in refs(models[k]):visit(ref)
for k in roots:visit(k)
groups={}
for k,m in models.items():
    clean={key:value for key,value in m.items() if key not in ('credit','format_version')}
    signature=json.dumps(clean,sort_keys=True)
    groups.setdefault(signature,[]).append(k)
print('UNREACHABLE CANDIDATES (verify dynamic references before removal):')
print('\n'.join(sorted(set(models)-seen)))
print('\nEXACT DUPLICATE GROUPS:')
for group in groups.values():
    if len(group)>1:print(' | '.join(group))
