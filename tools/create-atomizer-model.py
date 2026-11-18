"""Native fan geometry adapted for a rear rotor and a front fluid display."""
import json
from pathlib import Path
from zipfile import ZipFile
from io import BytesIO
from PIL import Image
ROOT=Path(__file__).resolve().parents[1]
A=ROOT/'src/main/resources/assets/ccd'
def save(path,data):
    path.parent.mkdir(parents=True,exist_ok=True); path.write_text(json.dumps(data,indent=2)+'\n',encoding='utf-8')
with ZipFile(next((Path.home()/'.gradle/caches/modules-2/files-2.1/com.simibubi.create/create-1.21.1/6.0.10-280').rglob('*-slim.jar'))) as z:
    image=Image.open(BytesIO(z.read('assets/create/textures/block/gearbox.png'))).convert('RGBA')
    copper=Image.open(A/'textures/block/booster_pump/body.png').convert('RGBA')
    for y in range(16):
        for x in range(16):
            r,g,b,a=image.getpixel((x,y))
            if r>g>b: image.putpixel((x,y),copper.getpixel((x,y)))
    target=A/'textures/block/fluid_atomizer/back.png'; target.parent.mkdir(parents=True,exist_ok=True); image.save(target)
    rotor=json.loads(z.read('assets/create/models/block/encased_fan/propeller.json'))
    for e in rotor['elements']:
        for k in ['from','to']: e[k][2]+=4
        if 'rotation' in e: e['rotation']['origin'][2]+=4
    save(A/'models/block/fluid_atomizer/propeller.json',rotor)
    for kind in ['block','item']:
        model=json.loads(z.read(f'assets/create/models/block/encased_fan/{kind}.json'))
        model['textures'].update(fan_side='create:block/copper_casing',back='ccd:block/fluid_atomizer/back',particle='create:block/copper_casing')
        if kind=='item':
            model['textures']['2']='ccd:block/fluid_atomizer/back'
            for e in model['elements']:
                if e.get('name','').lower() in ['fan','shaft']:
                    for k in ['from','to']: e[k][2]+=4
                    if 'rotation' in e: e['rotation']['origin'][2]+=4
        save(A/('models/block/fluid_atomizer.json' if kind=='block' else 'models/item/fluid_atomizer.json'),model)
