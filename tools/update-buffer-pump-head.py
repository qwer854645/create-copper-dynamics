"""Replace the supplied head and synchronize its namespaced inventory/combined elements."""
import json,sys,shutil
from pathlib import Path
root=Path(__file__).resolve().parents[1]
assets=root/'src/main/resources/assets/ccd'
source=Path(sys.argv[1])
head=assets/'models/block/buffer_pump/pump_head.json'
backup=root/'outputs/buffer-pump-head-revision-backup'
backup.mkdir(parents=True,exist_ok=True)
if not (backup/'pump_head.json').exists():shutil.copy2(head,backup/'pump_head.json')
shutil.copy2(source,head)
new=json.loads(head.read_text(encoding='utf-8'))
for relative in ['block/buffer_pump','item/buffer_pump']:
    path=assets/('models/'+relative+'.json')
    model=json.loads(path.read_text(encoding='utf-8'))
    # Combined models use part1_ exclusively for the pump_head component.
    model['elements']=[e for e in model['elements'] if not any(f['texture'].startswith('#part1_') for f in e['faces'].values())]
    model['textures']={k:v for k,v in model['textures'].items() if not k.startswith('part1_')}
    for k,v in new['textures'].items():model['textures']['part1_'+k]='#part1_'+v[1:] if v.startswith('#') else v
    elements=json.loads(json.dumps(new['elements']))
    for e in elements:
        for f in e['faces'].values():f['texture']='#part1_'+f['texture'][1:];f.pop('cullface',None)
    # tank is the first 14 elements; retain the component ordering.
    model['elements'][14:14]=elements
    path.write_text(json.dumps(model,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
