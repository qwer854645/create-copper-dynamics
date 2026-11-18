"""Assemble real RECTANGLE texture selections for reported tank dimensions."""
from pathlib import Path
from PIL import Image, ImageDraw

root=Path(__file__).resolve().parents[1]
sheet=Image.open(root/'src/main/resources/assets/ccd/textures/block/smart_fluid_tank/fluid_tank_connected.png').convert('RGBA')
out=Image.new('RGBA',(960,300),'#e5e8e2')
draw=ImageDraw.Draw(out)
for index,(width,height) in enumerate(((2,1),(3,1),(1,3),(3,3))):
    panel=Image.new('RGBA',(width*16,height*16))
    for y in range(height):
        for x in range(width):
            left,right=x>0,x<width-1
            up,down=y>0,y<height-1
            col=2 if left and right else 3 if left else 1 if right else 0
            row=1 if up and down else 2 if up else 0 if down else 3
            panel.paste(sheet.crop((col*16,row*16,col*16+16,row*16+16)),(x*16,y*16))
    scale=4
    out.alpha_composite(panel.resize((width*16*scale,height*16*scale),Image.Resampling.NEAREST),(index*240+20,40))
    draw.text((index*240+20,260),f'{width} wide x {height} high',fill='black')
out.save(root/'outputs/brass-edge-tanks/ct-face-cases.png')
