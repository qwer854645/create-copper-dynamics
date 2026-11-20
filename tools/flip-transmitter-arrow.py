"""Reverse only the lower atlas arrow panel; preserve filter and red rails."""
from pathlib import Path
from PIL import Image
root=Path(__file__).resolve().parents[1]
folder=root/'src/main/resources/assets/ccd/textures/block/fluid_transmitter'
for name in ('side','side_unpowered','side_powered'):
    path=folder/(name+'.png')
    image=Image.open(path).convert('RGBA')
    # The small neck samples atlas rows 10..13, columns 2..13.
    panel=image.crop((2,10,14,14)).transpose(Image.Transpose.ROTATE_180)
    image.paste(panel,(2,10))
    image.save(path)
