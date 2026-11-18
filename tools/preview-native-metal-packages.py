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
    sizes=['10x8','12x10','10x12','12x12']
    natives={size:json.loads(z.read(f'assets/create/models/item/package/cardboard_{size}.json')) for size in sizes}
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
            # Brass casing perimeter: brown outside fold, broad yellow inner lip.
            # Preserve the native package center, handling marks and sealing tape.
            casing=Image.open(BytesIO(z.read('assets/create/textures/block/brass_casing.png'))).convert('RGBA')
            regions=set()
            for source in natives.values():
                for face in source['elements'][0]['faces'].values():
                    u,v,U,V=face['uv']
                    regions.add((round(min(u,U)*4),round(min(v,V)*4),round(abs(U-u)*4),round(abs(V-v)*4)))
            for left,top,width,height in regions:
                for y in range(height):
                    for x in range(width):
                        depth=min(x,y,width-1-x,height-1-y)
                        if depth>1: continue
                        sx=x if x<2 else x+16-width if x>=width-2 else 2+round((x-2)*11/(width-5))
                        sy=y if y<2 else y+16-height if y>=height-2 else 2+round((y-2)*11/(height-5))
                        image.putpixel((left+x,top+y),casing.getpixel((sx,sy)))
        image.save(A/f'textures/item/native_package_{name}.png')
        model=json.loads(json.dumps(native)); model['textures']['0']=f'ccd:item/native_package_{name}'
        model['textures']['particle']=model['textures']['0']
        (A/f'models/item/native_package_{name}_study.json').write_text(json.dumps(model,indent=2)+'\n')
        assert model['elements']==native['elements']
        for size,source in natives.items():
            variant=json.loads(json.dumps(source))
            variant['textures']={'0':f'ccd:item/native_package_{name}','particle':f'ccd:item/native_package_{name}'}
            assert variant['elements']==source['elements']
            (A/f'models/item/native_package_{name}_{size}_study.json').write_text(json.dumps(variant,indent=2)+'\n')
        image.resize((512,512),Image.Resampling.NEAREST).save(OUT/f'{name}-atlas.png')
    (OUT/'source.txt').write_text('Create 6.0.10-280: cardboard_12x12 model and cardboard.png.\nCopper: palette-only derivative. Brass: palette derivative with original brass_casing two-pixel perimeter adapted to 12px faces.\nCreate-derived artwork, not original CCD artwork. Geometry and UV unchanged.\nUnregistered preview models only; runtime fluid_package and composite_package remain unchanged.\n')
