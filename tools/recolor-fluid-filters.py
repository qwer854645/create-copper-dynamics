"""Recolor only Create filter side frames; preserve centers and transparency."""
from pathlib import Path
import sys
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
REFERENCE = Path(sys.argv[1])
target = ROOT / 'src/main/resources/assets/ccd/textures/item'
target.mkdir(parents=True, exist_ok=True)
# Source pixels remain intact except the two vertical material strips.
copper = [(68, 35, 27), (101, 51, 35), (137, 70, 47),
          (171, 90, 61), (198, 112, 78), (224, 141, 105)]
previews = []
for source, name in [('filter', 'fluid_filter'), ('attribute_filter', 'fluid_attribute_filter')]:
    image = Image.open(REFERENCE / f'assets/create/textures/item/{source}.png').convert('RGBA')
    colors = sorted({image.getpixel((x, y))[:3] for x in range(image.width)
                     for y in range(image.height) if x < 3 or x >= image.width - 3
                     if image.getpixel((x, y))[3]}, key=lambda rgb: sum(rgb))
    palette = {rgb: copper[round(i * (len(copper)-1) / max(1, len(colors)-1))]
               for i, rgb in enumerate(colors)}
    for x in range(image.width):
        for y in range(image.height):
            r, g, b, a = image.getpixel((x, y))
            if a and (x < 3 or x >= image.width - 3):
                image.putpixel((x, y), (*palette[(r, g, b)], a))
    image.save(target / f'{name}.png')
    previews.append(image.resize((256, 256), Image.Resampling.NEAREST))
out = ROOT / 'outputs/fluid-filters'
out.mkdir(parents=True, exist_ok=True)
preview = Image.new('RGBA', (512, 256), '#e5e8e2')
for i, image in enumerate(previews):
    preview.alpha_composite(image, (i * 256, 0))
preview.save(out / 'items.png')
