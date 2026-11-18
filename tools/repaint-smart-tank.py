"""Use the mechanical arm's actual brass ramp; preserve tank alpha and CT tile layout."""
from pathlib import Path
from PIL import Image
from zipfile import ZipFile
from io import BytesIO
import sys
sys.path.insert(0,str(Path.home()/'.config/opencode/skills/pixel-art/scripts'))
from pxl import Canvas
ROOT=Path(__file__).resolve().parents[1]
folder=ROOT/'src/main/resources/assets/ccd/textures/block/smart_fluid_tank'
old=[(68,50,42),(114,71,49),(158,105,71),(175,121,79),(191,142,85),(206,160,90),(228,183,99),(247,203,108),(255,235,140)]
new=[(114,71,49),(164,107,71),(175,121,79),(199,151,88),(215,170,94),(228,183,99),(247,203,108),(251,220,125),(255,235,140)]
archive=ZipFile(next((Path.home()/'.gradle/caches/modules-2/files-2.1/com.simibubi.create/create-1.21.1/6.0.10-280').rglob('*-slim.jar')))
out=ROOT/'outputs/smart-fluid-tank'
for path in folder.glob('*.png'):
    image=Image.open(BytesIO(archive.read('assets/create/textures/block/'+path.name))).convert('RGBA'); canvas=Canvas(image.width,image.height)
    for y in range(image.height):
        for x in range(image.width):
            r,g,b,a=image.getpixel((x,y))
            color=(r,g,b)
            if a and r>=g and g>=b and r-b>=20 and g-b>=8:
                luminance=.2126*r+.7152*g+.0722*b
                index=next((i for i,t in enumerate([35,55,75,90,108,128,150,180]) if luminance<t),8)
                color=new[index]
            canvas.set(x,y,(*color,a))
    canvas.save(str(path))
images=[Image.open(folder/(n+'.png')).convert('RGBA') for n in ['fluid_tank','fluid_tank_top','fluid_tank_inner','fluid_tank_window_single']]
sheet=Image.new('RGBA',(sum(i.width for i in images),max(i.height for i in images)))
x=0
for image in images: sheet.alpha_composite(image,(x,0)); x+=image.width
sheet.resize((sheet.width*8,sheet.height*8),Image.Resampling.NEAREST).save(out/'arm-brass-textures.png')
