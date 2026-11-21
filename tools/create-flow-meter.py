"""Copper derivative of Create's gauge texture; preserve dial and needle regions."""
from pathlib import Path
from PIL import Image
import sys
root=Path(__file__).resolve().parents[1]
reference=Path(sys.argv[1])
image=Image.open(reference/'assets/create/textures/block/gauge.png').convert('RGBA')
palette=[(65,32,24),(91,44,30),(119,59,39),(145,73,48),(171,89,60),(195,108,76),(216,130,96),(231,153,118)]
for y in range(image.height):
    for x in range(image.width):
        if (x<16 and y<14) or y>=18:
            r,g,b,a=image.getpixel((x,y))
            if a and max(r,g,b)>45:
                light=(r+g+b)/3
                index=max(0,min(7,round((light-40)/24)))
                image.putpixel((x,y),(*palette[index],a))
out=root/'src/main/resources/assets/ccd/textures/block/flow_meter.png'
out.parent.mkdir(parents=True,exist_ok=True)
image.save(out)
