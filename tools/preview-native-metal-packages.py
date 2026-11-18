"""Native package palette-only study; registered carriers are unchanged."""
import json
from pathlib import Path
from zipfile import ZipFile
from io import BytesIO
from PIL import Image
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
        if name=='brass':
            # Mechanical-arm style brown folds, confined to structural seams.
            # Preserve paper-derived face grain and keep a narrow adjacent gold lip.
            for tile in range(5):
                left,top=36,1+tile*12
                # Original eye/handling-symbol side panels keep their native pixels.
                # Do not run a dark strap through these faces.
                if tile in (0,1):
                    continue
                if tile<3:
                    for x in range(12):
                        image.putpixel((left+x,top+11),(*ramp[2],255))
                    for x in range(12):
                        image.putpixel((left+x,top+1),(*ramp[3],255))
                    for y in range(12): image.putpixel((left+7,top+y),(*ramp[3],255))
                else:
                    for y in range(12): image.putpixel((left+7,top+y),(*ramp[3],255))
                    for x in range(12):
                        image.putpixel((left+x,top+5),(*ramp[3],255))
                    # Continuous perimeter fold meets the vertical face bands at corners.
                    for x in range(12):
                        image.putpixel((left+x,top),(*ramp[3],255))
                        image.putpixel((left+x,top+11),(*ramp[3],255))
                    for y in range(12):
                        image.putpixel((left,top+y),(*ramp[3],255))
                        image.putpixel((left+11,top+y),(*ramp[3],255))
        image.save(A/f'textures/item/native_package_{name}.png')
        model=json.loads(json.dumps(native)); model['textures']['0']=f'ccd:item/native_package_{name}'
        model['textures']['particle']=model['textures']['0']
        (A/f'models/item/native_package_{name}_study.json').write_text(json.dumps(model,indent=2)+'\n')
        assert model['elements']==native['elements']
        image.resize((512,512),Image.Resampling.NEAREST).save(OUT/f'{name}-atlas.png')
    (OUT/'source.txt').write_text('Create 6.0.10-280: cardboard_12x12 model and cardboard.png.\nPalette derivatives of Create artwork; not original CCD artwork. Geometry and UV unchanged.\nUnregistered preview models only; runtime fluid_package and composite_package remain unchanged.\n')
