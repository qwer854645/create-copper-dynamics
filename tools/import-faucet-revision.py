"""Import user faucet revisions and synchronize the smart inventory body."""
import copy,json,shutil
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
MODELS=ROOT/'src/main/resources/assets/ccd/models'
BACKUP=ROOT/'outputs/faucet-revision/backup'
for name in ('faucet_closed','faucet_open'):
    path=MODELS/f'block/{name}.json'
    backup=BACKUP/f'{name}.json';backup.parent.mkdir(parents=True,exist_ok=True)
    if not backup.exists(): shutil.copy2(path,backup)
    data=json.loads((Path.home()/'Downloads'/f'{name}.json').read_text(encoding='utf-8-sig'))
    data['render_type']='minecraft:cutout_mipped'
    path.write_text(json.dumps(data,indent=2)+'\n',encoding='utf-8')
path=MODELS/'item/smart_faucet.json'
backup=BACKUP/'smart_faucet-item.json'
if not backup.exists():shutil.copy2(path,backup)
model=json.loads(path.read_text(encoding='utf-8'))
panel=[copy.deepcopy(e) for e in model['elements'] if e.get('name')=='front_filter_panel']
body=json.loads((MODELS/'block/faucet_closed.json').read_text(encoding='utf-8'))
model['elements']=copy.deepcopy(body['elements'])+panel
path.write_text(json.dumps(model,indent=2)+'\n',encoding='utf-8')
