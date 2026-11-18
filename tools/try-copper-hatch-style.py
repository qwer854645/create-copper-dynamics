"""Copper hatch study: native Create UVs, continuous copper folds and dark recesses.

Experience Hatch is a visual reference only; no pixels or geometry are imported.
"""
import io
import sys
from pathlib import Path
from zipfile import ZipFile
from PIL import Image

sys.path.insert(0, str(Path.home()/'.config/opencode/skills/pixel-art/scripts'))
from pxl import Canvas

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT/'outputs/copper-hatch-study'
OUT.mkdir(parents=True, exist_ok=True)
DEST = ROOT/'src/main/resources/assets/ccd/textures/block/fluid_hatch'
JAR = next((Path.home()/'.gradle/caches/modules-2/files-2.1/com.simibubi.create/create-1.21.1/6.0.10-280').rglob('*-slim.jar'))
COPPER = ['#4a251f', '#693329', '#793b2b', '#904931', '#a75a40', '#b26247', '#c26b4c', '#d67b5b', '#e3826c']

def rgb(value):
    return tuple(bytes.fromhex(value[1:]))

with ZipFile(JAR) as archive:
    for target, native in [('housing', 'hatch'), ('flap', 'hatch_closed')]:
        previous = Image.open(DEST/(target+'.png')).convert('RGBA')
        backup = OUT/(target+'-before.png')
        if not backup.exists():
            previous.save(backup)
        source = Image.open(io.BytesIO(archive.read('assets/create/textures/block/'+native+'.png'))).convert('RGBA')
        canvas = Canvas(source.width, source.height)
        for y in range(source.height):
            for x in range(source.width):
                pixel = source.getpixel((x, y))
                if pixel[3]:
                    luminance = sum(pixel[:3])/3
                    # Native tonal clusters retained, with a copper rather than iron frame.
                    index = min(8, max(0, round((luminance-28)/17)))
                    color = rgb(COPPER[index]) + (pixel[3],)
                    if target == 'flap':
                        # Keep the original handle legible; quiet copper panel below it.
                        color = previous.getpixel((x, y))
                    canvas.set(x, y, color)
        if target == 'housing':
            # UV [1.5,9.5,7.5,15.1]: inner underside. Broad dark copper
            # clusters replace repeated rail-like stripes, with a narrow folded rim.
            for y in range(19, 30):
                for x in range(3, 15):
                    if source.getpixel((x,y))[3]:
                        edge = min(x-3,14-x,y-19,29-y)
                        tone = 5 if edge == 0 else 2 if edge == 1 else 1
                        if edge > 1 and x < 8 and y < 25:
                            tone = 2
                        canvas.set(x,y,rgb(COPPER[tone])+(source.getpixel((x,y))[3],))
            # UV side triangles: a continuous copper sheet with a lit top fold.
            for y in range(19, 30):
                for x in range(19, 30):
                    alpha=source.getpixel((x,y))[3]
                    if not alpha:
                        continue
                    tone=6 if y==19 else 4 if y==20 else 3 if x+y<48 else 2
                    canvas.set(x,y,rgb(COPPER[tone])+(alpha,))
        assert canvas.img.getchannel('A').tobytes()==source.getchannel('A').tobytes()
        canvas.save(DEST/(target+'.png'))
        canvas.resize(12).save(OUT/(target+'-after.png'))
print('Copper hatch textures updated; native alpha preserved; smart hatch untouched.')
