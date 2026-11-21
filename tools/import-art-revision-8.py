"""Import revision 8 artwork, excluding stale inventory copies from revision 7."""
import json
from pathlib import Path
from zipfile import ZipFile
import sys

root=Path(__file__).resolve().parents[1]
assets=root/'src/main/resources/assets/ccd'
skip={'models/item/flow_meter.json','models/item/vertical_turning_booster_pump.json'}
changed=[]
with ZipFile(Path(sys.argv[1])) as archive:
    for name in archive.namelist():
        prefix='aaa/assets/ccd/'
        if not name.startswith(prefix) or name.endswith('/'):continue
        relative=Path(name[len(prefix):])
        if '..' in relative.parts:raise ValueError(name)
        if relative.as_posix() in skip:continue
        path=assets/relative
        content=archive.read(name)
        if path.exists() and path.read_bytes()==content:continue
        backup=root/'outputs/art-revision-8-backup'/relative
        if path.exists() and not backup.exists():
            backup.parent.mkdir(parents=True,exist_ok=True)
            backup.write_bytes(path.read_bytes())
        path.parent.mkdir(parents=True,exist_ok=True)
        path.write_bytes(content)
        changed.append(relative.as_posix())

path=assets/'models/block/creative_delivery_fluid_tank.json'
model=json.loads(path.read_text(encoding='utf-8'))
lid=next(e for e in model['elements'] if e.get('name')=='Lid')
for side in ['north','south','east','west']:
    lid['faces'][side]['cullface']=side
path.write_text(json.dumps(model,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print('Imported changes:\n'+'\n'.join(changed))
print('Kept synchronized flow meter and vertical turning pump inventory models; corrected lid cull directions.')
