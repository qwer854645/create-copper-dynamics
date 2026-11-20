"""Use native green indicator pixels for the pump's two reachable dial stops."""
import json
from pathlib import Path
from PIL import Image
root=Path(__file__).resolve().parents[1]/'src/main/resources/assets/ccd'
reference=Path.home()/'AppData/Local/Temp/opencode/create-art-reference/assets/create/textures/block/valve_open.png'
image=Image.open(reference).convert('RGBA')
green=[image.getpixel((x,y)) for y in (7,8) for x in (3,4,5)]
for y in (3,4,5,10,11,12):
    for x in (7,8):
        image.putpixel((x,y),green[((y%3)*2+(x-7))%len(green)])
target=root/'textures/block/redstone_fluid_pump/dial.png'
target.parent.mkdir(parents=True,exist_ok=True)
image.save(target)
for rel in ('models/block/redstone_fluid_pump/block.json','models/item/redstone_fluid_pump.json'):
    path=root/rel
    data=json.loads(path.read_text(encoding='utf-8'))
    data['textures']['dial']='ccd:block/redstone_fluid_pump/dial'
    path.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
