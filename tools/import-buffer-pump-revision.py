"""Import the supplied assembled item and cog without changing geometry or UVs."""
from pathlib import Path
import json
import shutil
from zipfile import ZipFile

root=Path(__file__).resolve().parents[1]
models=root/'src/main/resources/assets/ccd/models'
downloads=Path(r'C:\Users\Administrator\Downloads')
backup=root/'outputs/buffer-pump-revision/backup'
backup.mkdir(parents=True,exist_ok=True)
for name in ['item/buffer_pump.json','block/buffer_pump/cog.json','block/buffer_pump/tank.json','block/buffer_pump/pump_head.json']:
    target=backup/name
    target.parent.mkdir(parents=True,exist_ok=True)
    if not target.exists():shutil.copy2(models/name,target)
item=json.loads((downloads/'buffer_pump.json').read_text())
cog=json.loads((downloads/'cog (2).json').read_text())
texture=root/'src/main/resources/assets/ccd/textures/block/reciprocating_fluid_pump.png'
if not texture.exists():
    with ZipFile(downloads/'aaa (9).zip') as archive:
        texture.write_bytes(archive.read('aaa/assets/ccd/textures/block/reciprocating_fluid_pump.png'))
assert sorted(json.dumps(e,sort_keys=True) for e in cog['elements'])==sorted(
    json.dumps(e,sort_keys=True) for e in item['elements'] if e.get('name')=='native_pump_cog')
shutil.copy2(downloads/'buffer_pump.json',models/'item/buffer_pump.json')
shutil.copy2(downloads/'cog (2).json',models/'block/buffer_pump/cog.json')
for part,names in [('tank',{'compressed_tank'}),('pump_head',{'native_pump_rim_up'})]:
    elements=[e for e in item['elements'] if e.get('name') in names and (part!='pump_head' or e['from']==[2,13,2])]
    data={'parent':'minecraft:block/block','render_type':'minecraft:cutout_mipped',
          'textures':item['textures'],'elements':elements}
    (models/f'block/buffer_pump/{part}.json').write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print('Imported exact item and cog; static tank/head synchronized from the supplied item.')
