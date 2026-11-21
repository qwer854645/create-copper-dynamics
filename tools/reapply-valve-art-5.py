"""Apply the user's signed dial geometry without normalizing intentional inward faces."""
from pathlib import Path
from zipfile import ZipFile
import json

root=Path(__file__).resolve().parents[1]
assets=root/'src/main/resources/assets/ccd'
backup=root/'outputs/valve-art-5-reapply/backup'
models=(
 'models/block/redstone_fluid_pump/block.json',
 'models/block/redstone_fluid_pump/pointer.json',
 'models/item/redstone_fluid_pump.json',
 'models/block/three_way_valve/dial_powered.json',
 'models/block/three_way_valve/dial_unpowered.json',
 'models/block/three_way_valve/pointer.json',
 'models/item/three_way_valve.json',
)
with ZipFile(Path.home()/'Downloads/aaa (5).zip') as archive:
 for rel in models:
  path=assets/rel;old=backup/rel
  if path.exists() and not old.exists():
   old.parent.mkdir(parents=True,exist_ok=True);old.write_bytes(path.read_bytes())
  data=json.loads(archive.read('aaa/assets/ccd/'+rel))
  # The earlier request for green indicators remains in force. The extra
  # supplied stop uses the same approved green source, not valve_closed red.
  if 'redstone_fluid_pump' in rel:
   for key,value in list(data.get('textures',{}).items()):
    if value in ('create:block/valve_open','create:block/valve_closed'):
     data['textures'][key]='ccd:block/redstone_fluid_pump/dial'
  path.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print('Reapplied seven model resources with signed dial bounds and original UVs; green pump indicators retained.')
print('Run tools/sync-valve-item-models.cjs after importing to keep inventory parts aligned with runtime rendering.')
