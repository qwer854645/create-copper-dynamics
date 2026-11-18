"""Native package geometry with redrawn CCD metal panels and folded sealing strips."""
import json
import sys
from pathlib import Path
from zipfile import ZipFile
from io import BytesIO
from PIL import Image
sys.path.insert(0,str(Path.home()/'.config/opencode/skills/pixel-art/scripts'))
from pxl import Canvas
ROOT=Path(__file__).resolve().parents[1]
A=ROOT/'src/main/resources/assets/ccd'
OUT=ROOT/'outputs/native-metal-packages'; OUT.mkdir(parents=True,exist_ok=True)
with ZipFile(next((Path.home()/'.gradle/caches/modules-2/files-2.1/com.simibubi.create/create-1.21.1/6.0.10-280').rglob('*-slim.jar'))) as z:
    original=Image.open(BytesIO(z.read('assets/create/textures/item/package/cardboard.png'))).convert('RGBA')
    native=json.loads(z.read('assets/create/models/item/package/cardboard_12x12.json'))
    ramps={
        'copper':[(70,32,24),(121,59,43),(144,73,49),(154,80,56),(167,90,64),(178,98,71),(194,107,76),(200,116,86),(214,123,91),(227,130,108)],
        'brass':[(73,47,27),(114,71,49),(164,107,71),(175,121,79),(199,151,88),(215,170,94),(228,183,99),(247,203,108),(251,220,125),(255,235,140)]}
    values=sorted(set(round(.2126*r+.7152*g+.0722*b,3) for r,g,b,a in original.getdata() if a))
    for name,ramp in ramps.items():
        image=original.copy()
        for y in range(image.height):
            for x in range(image.width):
                r,g,b,a=original.getpixel((x,y))
                if not a: continue
                value=round(.2126*r+.7152*g+.0722*b,3)
                index=round(values.index(value)/(len(values)-1)*(len(ramp)-1))
                image.putpixel((x,y),(*ramp[index],a))
        texture=A/f'textures/item/native_package_{name}.png'; image.save(texture)
        # Native 12x12 model uses five 12px tiles at atlas x=36, y=1+12*n.
        # Draw metal afresh instead of retaining paper grain/creases from the source.
        panels={
            'copper':[
                '277777777772','765555655553','754445655443','754334554443',
                '754334444553','755444433553','765554433443','754444445543',
                '754334455543','754334444443','755444433453','133333333331'],
            'brass':[
                '388888888883','876666776664','865555776554','865566655554',
                '866666555664','865666555664','865566776554','865555776554',
                '866655665554','876655556664','865555555554','244444444442']}
        for tile in range(5):
            c=Canvas(12,12)
            for y,row in enumerate(panels[name]):
                for x,index in enumerate(row):
                    c.set(x,y,ramp[int(index)])
            # Flush metal sealing band, lighter folded lip with dark right edge.
            if tile in (0,1,2):
                for y in range(12):
                    c.set(5,y,ramp[7 if name=='copper' else 8])
                    c.set(6,y,ramp[5 if name=='copper' else 6])
                    c.set(7,y,ramp[2])
                # Horizontal lid fold is defined through pixels, no extra cuboids.
                for x in range(1,11):
                    if x not in (5,6,7): c.set(x,2,ramp[2])
            else:
                for y in range(12):
                    c.set(5,y,ramp[7 if name=='copper' else 8]); c.set(6,y,ramp[5 if name=='copper' else 6]); c.set(7,y,ramp[2])
                # Inset lid seam following original folded package construction.
                for x in list(range(1,5))+list(range(8,11)):
                    c.set(x,5,ramp[2]); c.set(x,6,ramp[6])
            image.alpha_composite(c.img,(36,1+tile*12))
        image.save(texture)
        model=json.loads(json.dumps(native)); model['textures']['0']=f'ccd:item/native_package_{name}'
        model['textures']['particle']=model['textures']['0']
        (A/f'models/item/native_package_{name}_study.json').write_text(json.dumps(model,indent=2)+'\n')
        assert model['elements']==native['elements']
        image.resize((512,512),Image.Resampling.NEAREST).save(OUT/f'{name}-atlas.png')
    (OUT/'source.txt').write_text('Create 6.0.10-280 cardboard_12x12 geometry and UV unchanged.\nVisible 12x12 panels redrawn with pixel-art Canvas: Create pump/copper casing palette; mechanical arm brass palette.\nOriginal folded box concept retained, paper grain replaced by metal clusters, folded rims and sealing bands.\nUnused atlas regions retain palette-derived Create artwork; geometry belongs to Create.\nUnregistered preview models only; runtime carrier models unchanged.\n')
