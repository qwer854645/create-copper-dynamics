"""Import user artwork exactly and synchronize CCD repackager state/item references."""
from pathlib import Path
from zipfile import ZipFile
import json, shutil
root=Path(__file__).resolve().parents[1]
resources=root/'src/main/resources'
backup=root/'outputs/aaa10-art-backup'
archive=Path(r'C:\Users\Administrator\Downloads\aaa (10).zip')
def save(relative,data):
    target=resources/relative
    old=backup/relative
    if target.exists() and not old.exists():
        old.parent.mkdir(parents=True,exist_ok=True)
        shutil.copy2(target,old)
    target.parent.mkdir(parents=True,exist_ok=True)
    target.write_bytes(data)
def model(relative,data):
    save('assets/ccd/models/'+relative,(json.dumps(data,indent=2)+'\n').encode())
with ZipFile(archive) as z:
    for entry in z.infolist():
        if entry.is_dir():continue
        relative=entry.filename.removeprefix('aaa/')
        if relative.startswith('assets/ccd/'):
            assert '..' not in Path(relative).parts
            save(relative,z.read(entry))
jar=next((Path.home()/'.gradle/caches/modules-2/files-2.1/com.simibubi.create/create-1.21.1/6.0.10-280').rglob('*-slim.jar'))
textures={'0':'create:block/packager_frame','1':'ccd:block/fluid_packager/packager_details',
          '2':'ccd:block/fluid_packager/packager_horizontal_unpowered',
          '5':'ccd:block/fluid_packager/packager_vertical_unpowered',
          'particle':'ccd:block/fluid_packager/packager_particle'}
model('block/fluid_repackager/block_powered.json',{
    'parent':'ccd:block/fluid_repackager/block',
    'textures':{'2':'ccd:block/fluid_packager/packager_horizontal_powered'}})
model('block/fluid_repackager/block_vertical.json',{
    'parent':'create:block/repackager/block_vertical','render_type':'minecraft:cutout_mipped',
    'ambientocclusion':False,'textures':textures})
model('block/fluid_repackager/block_vertical_powered.json',{
    'parent':'ccd:block/fluid_repackager/block_vertical',
    'textures':{'5':'ccd:block/fluid_packager/packager_vertical_powered'}})
with ZipFile(jar) as z:item=json.loads(z.read('assets/create/models/block/repackager/item.json'))
item['render_type']='minecraft:cutout_mipped'
item['textures'].update({k:v for k,v in textures.items() if k!='5'})
for element in item['elements']:
    if element['from']==[2,2,2] and element['to']==[14,4,14]:
        element['faces']['up']={'uv':[9,9,15,15],'texture':'#2'}
    elif element['from']==[0,4,0] and element['to']==[16,4,16]:
        element['faces']['up']={'uv':[8,8,16,16],'texture':'#2'}
model('item/fluid_repackager.json',item)
print('Imported 12 user assets; synchronized powered, vertical and item models.')
