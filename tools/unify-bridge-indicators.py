"""Use powered artwork geometry for both states; darken only red lamp pixels."""
from pathlib import Path
import sys
sys.path.insert(0,str(Path.home()/'.config/opencode/skills/pixel-art/scripts'))
from pxl import Canvas
root=Path(__file__).resolve().parents[1]
assets=root/'src/main/resources/assets/ccd/textures/block/fluid_access'
backup=root/'outputs/item-display-repair/backup'
backup.mkdir(parents=True,exist_ok=True)
for name in ('bridge_band','connected_bridge'):
    target=assets/(name+'_unpowered.png')
    old=backup/target.name
    if not old.exists():old.write_bytes(target.read_bytes())
    canvas=Canvas.open(str(assets/(name+'_powered.png')))
    for y in range(16):
        for x in range(16):
            r,g,b,a=canvas.get(x,y)
            if a and r>g*2 and r>b*2 and r>70 and g<65 and b<65:
                canvas.set(x,y,(round(r*.32),round(g*.32),round(b*.32),a))
    canvas.save(str(target))
