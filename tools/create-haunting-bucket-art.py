"""Independent soul-fluid icon using the approved bucket body and soul-fluid palette."""
from pathlib import Path
import sys, json
from PIL import Image
sys.path.insert(0,r'C:\Users\Administrator\.config\opencode\skills\pixel-art\scripts')
from pxl import Canvas
root=Path(__file__).resolve().parents[1]
assets=root/'src/main/resources/assets/ccd'
source=assets/'textures/item/conduit_energy_fluid_bucket.png'
c=Canvas.open(str(source))
# Preserve all metal/outline pixels. Only recolor the existing saturated liquid mask.
for y in range(16):
    for x in range(16):
        r,g,b,a=c.get(x,y)
        if a and g>r*1.3 and b>r*1.3:
            color='#398c96' if g<130 else '#59c8c5' if g<175 else '#91e4d7'
            c.set(x,y,color)
c.save(str(assets/'textures/item/haunting_bucket.png'))
(assets/'models/item/haunting_bucket.json').write_text(json.dumps({
    'parent':'minecraft:item/generated','textures':{'layer0':'ccd:item/haunting_bucket'}},indent=2)+'\n')
out=root/'outputs/haunting-bucket-art';out.mkdir(parents=True,exist_ok=True)
Image.open(assets/'textures/item/haunting_bucket.png').resize((256,256),Image.Resampling.NEAREST).save(out/'preview.png')
print('Created 16x16 static soul fluid bucket; bucket body and transparency preserved.')
