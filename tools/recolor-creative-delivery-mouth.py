"""Create-derived interface atlas, recolored to native creative casing palette."""
from pathlib import Path
import sys,json
from PIL import Image
sys.path.insert(0,str(Path.home()/'.config/opencode/skills/pixel-art/scripts'))
from pxl import Canvas
root=Path(__file__).resolve().parents[1]
reference=Path.home()/'AppData/Local/Temp/opencode/create-art-reference/assets/create/textures/block'
image=Image.open(reference/'portable_fluid_interface.png').convert('RGBA')
creative=Image.open(reference/'creative_casing.png').convert('RGBA')
palette=sorted(set(creative.getdata()),key=lambda p:sum(p[:3]))
copper=sorted({p for p in image.getdata() if p[3] and p[0]>p[1]*1.2 and p[0]>p[2]*1.2},key=lambda p:sum(p[:3]))
mapping={color:palette[round(i*(len(palette)-1)/max(1,len(copper)-1))] for i,color in enumerate(copper)}
canvas=Canvas.open(str(reference/'portable_fluid_interface.png'))
for y in range(image.height):
 for x in range(image.width):
  pixel=image.getpixel((x,y))
  if pixel in mapping:canvas.set(x,y,(*mapping[pixel][:3],pixel[3]))
target=root/'src/main/resources/assets/ccd/textures/block/creative_delivery_fluid_tank/interface.png'
target.parent.mkdir(parents=True,exist_ok=True);canvas.save(str(target))
target.with_suffix('.meta.json').write_text(json.dumps({'source':'Create portable_fluid_interface.png','palette':'Create creative_casing.png','change':'Copper colors replaced; UV, size, alpha and neutral metal preserved.'},indent=2)+'\n')
